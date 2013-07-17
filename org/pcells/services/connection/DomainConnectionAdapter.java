// $Id: DomainConnectionAdapter.java,v 1.2 2006-11-19 09:14:19 patrick Exp $
//
package org.pcells.services.connection;
//
import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Timestamp;
import java.util.*;

import dmg.cells.applets.login.DomainObjectFrame;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.util.io.TeeInputStream;

/**
 */
public class DomainConnectionAdapter implements DomainConnection {

    private Map<DomainObjectFrame, DomainConnectionListener> _packetHash = new HashMap<DomainObjectFrame, DomainConnectionListener>();
    private final Object _ioLock = new Object();
    private int _ioCounter = 100;
    private List<DomainEventListener> _listener = new ArrayList<DomainEventListener>();
    private boolean _connected = false;
    private InputStream _inputStream = null;
    private OutputStream _outputStream = null;
    private Reader _reader = null;
    private Writer _writer = null;
    private ObjectOutputStream _objOut = null;
    private ObjectInputStream _objIn = null;

    public String getAuthenticatedUser() {
        return "Unknown";
    }

    public void setIoStreams(InputStream in, OutputStream out) {
        setIoStreams(in, out, null, null);
    }

    public void setIoStreams(InputStream in, OutputStream out,
            Reader reader, Writer writer) {

        _inputStream = in;
        _outputStream = out;
        _reader = reader;
        _writer = writer;

    }

    public void go() throws Exception {
        System.out.println("runConnection started");
        try {
            runConnection();

            System.out.println("runConnection OK");

            informListenersOpened();

            System.out.println("runReceiver starting");
            runReceiver();
        } catch (Throwable e) {
            writeToFile("Exception caught:"+e.toString());
        } finally {
            System.out.println("runReceiver finished");
            informListenersClosed();
        }
    }

    public void close() throws IOException {
        _objOut.close();
    }

    private static class MyFilter extends FilterInputStream {

        public MyFilter(InputStream in) {
            super(in);
        }

        public int read() throws IOException {
            int r = super.read();
            return r;
        }

        public int read(byte[] data, int offset, int len) throws IOException {
            int r = super.read(data, offset, 1);
            return r;
        }

        public int read(byte[] data) throws IOException {

            byte[] x = new byte[1];
            int r = super.read(x);
            data[0] = x[0];
            return r;
        }
    }

    private void runConnection() throws IOException {

        InputStream inputstream = new MyFilter(_inputStream);
        BufferedReader reader = new BufferedReader(
                _reader == null
                ? new InputStreamReader(inputstream)
                : _reader, 1);

        PrintWriter writer = new PrintWriter(_writer == null
                ? new OutputStreamWriter(_outputStream)
                : _writer);

        writer.println("$BINARY$");
        writer.flush();
        System.out.println("Wrote Binary");
        writeToFile("Wrote Binary");
        String check = null;
        do {

            check = reader.readLine();
            System.out.println("This was read from the InputStream: "+ check);

        } while (!check.equals("$BINARY$"));
        _objOut = new ObjectOutputStream(new BufferedOutputStream(_outputStream));
        _objOut.flush();
        Calendar calendar = Calendar.getInstance();
        Date currentTimestamp = new Timestamp(calendar.getTime().getTime());
        System.out.println(currentTimestamp.toString() + " Flushed ObjectOutputStream Opening object streams.");
        try {
            assert inputstream != null;
            OutputStream branchStream = new OutputStream() {
				
				@Override
				public void write(int b) throws IOException {
					System.out.println("Input DomainConnectionAdapter: "+ b);					
				}
			};
            TeeInputStream teeIn = new TeeInputStream(inputstream, branchStream);
            System.out.println("Created TeeInputStream");
//            BufferedInputStream bufStream = new BufferedInputStream(teeIn);
            _objIn = new ObjectInputStream(teeIn);
            System.out.println("Created ObjectStreams.");
        } catch (Exception e) {
            writeToFile("Exception while creating ObjectInputStream: "+e.toString());
        }
    }

    private void writeToFile(String s) {
        File f = new File("/tmp/debugOutput");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                System.out.println("File could not be created.");
            }
        }
        try {
            FileWriter fr = new FileWriter(f);
            fr = (FileWriter) fr.append(s);
            fr = (FileWriter) fr.append("\n");
            fr.flush();
            fr.close();
        } catch (IOException ex) {
            System.out.println("Exception: "+ ex);
        }
    }

    private void runReceiver() throws Exception {

        Object obj = null;
        DomainObjectFrame frame = null;
        DomainConnectionListener listener = null;

        while (true) {

            if ((obj = _objIn.readObject()) == null) {
                break;
            }
            if (!(obj instanceof DomainObjectFrame)) {
                continue;
            }

            synchronized (_ioLock) {

                frame = (DomainObjectFrame) obj;
                listener = _packetHash.remove(frame);
                if (listener == null) {
                    System.err.println("Message without receiver : " + frame);
                    continue;
                }
            }
            try {
                listener.domainAnswerArrived(frame.getPayload(), frame.getSubId());
            } catch (Exception eee) {
                System.out.println("Problem in domainAnswerArrived : " + eee);
            }
        }
    }

    public int sendObject(Object obj,
            DomainConnectionListener listener,
            int id) throws IOException {

        synchronized (_ioLock) {

            if (!_connected) {
                throw new IOException("Not connected");
            }

            DomainObjectFrame frame =
                    new DomainObjectFrame(obj, ++_ioCounter, id);

            _objOut.writeObject(frame);
            _objOut.reset();
            _packetHash.put(frame, listener);
            return _ioCounter;
        }
    }

    public int sendObject(String destination,
            Object obj,
            DomainConnectionListener listener,
            int id) throws IOException {
//         System.out.println("Sending : "+obj ) ;
        synchronized (_ioLock) {
            if (!_connected) {
                throw new IOException("Not connected");
            }
            DomainObjectFrame frame =
                    new DomainObjectFrame(destination, obj, ++_ioCounter, id);
            _objOut.writeObject(frame);
            _objOut.reset();
            _packetHash.put(frame, listener);
            return _ioCounter;
        }
    }

    public void addDomainEventListener(DomainEventListener listener) {
        synchronized (_ioLock) {
            _listener.add(listener);
            if (_connected) {
                try {
                    listener.connectionOpened(this);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    public void removeDomainEventListener(DomainEventListener listener) {
        synchronized (_ioLock) {
            _listener.remove(listener);
        }
    }

    private void informListenersOpened() {
        List<DomainEventListener> array = new ArrayList<DomainEventListener>(_listener);
        synchronized (_ioLock) {
            _connected = true;
            for (DomainEventListener listener : array) {

                try {
                    listener.connectionOpened(this);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }

    private void informListenersClosed() {
        List<DomainEventListener> array = new ArrayList<DomainEventListener>(_listener);
        synchronized (_ioLock) {
            _connected = false;
            for (DomainEventListener listener : array) {
                try {
                    listener.connectionClosed(this);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
    }
}
