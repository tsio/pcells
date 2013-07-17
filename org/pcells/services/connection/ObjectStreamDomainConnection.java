package org.pcells.services.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import dmg.cells.applets.login.DomainObjectFrame;

public abstract class ObjectStreamDomainConnection extends AbstractDomainConnection
{
    private final Object _ioLock = new Object();
    private ObjectOutputStream _objOut;
    private ObjectInputStream _objIn;
    private Map<DomainObjectFrame, DomainConnectionListener> _packetHash =
            new HashMap<DomainObjectFrame, DomainConnectionListener>();
    private int _ioCounter = 100;

    @Override
    public void close() throws IOException
    {
        synchronized (_ioLock) {
            _objOut.close();
            _objIn.close();
        }
    }

    public void run(InputStream in, OutputStream out) throws IOException, ClassNotFoundException
    {
        _objOut = new ObjectOutputStream(out);
        _objOut.flush();
        _objIn = new ObjectInputStream(in);

        connected();
        System.out.println("runReceiver starting");
        try {
            Object obj;
            while ((obj = _objIn.readObject()) != null) {
                if (obj instanceof DomainObjectFrame) {
                    DomainConnectionListener listener;
                    DomainObjectFrame frame;
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
        } finally {
            System.out.println("runReceiver finished");
            disconnected();
        }
    }

    @Override
    public int sendObject(Object obj,
            DomainConnectionListener listener,
            int id) throws IOException
    {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }
        synchronized (_ioLock) {
            DomainObjectFrame frame =
                    new DomainObjectFrame(obj, ++_ioCounter, id);

            _objOut.writeObject(frame);
            _objOut.reset();
            _packetHash.put(frame, listener);
            return _ioCounter;
        }
    }

    @Override
    public int sendObject(String destination,
            Object obj,
            DomainConnectionListener listener,
            int id) throws IOException
    {
        if (!isConnected()) {
            throw new IOException("Not connected");
        }
        synchronized (_ioLock) {
            DomainObjectFrame frame =
                    new DomainObjectFrame(destination, obj, ++_ioCounter, id);
            _objOut.writeObject(frame);
            _objOut.reset();
            _packetHash.put(frame, listener);
            return _ioCounter;
        }
    }
}
