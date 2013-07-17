// $Id: DomainConnectionAdapter.java,v 1.2 2006-11-19 09:14:19 patrick Exp $
//
package org.pcells.services.connection;
//
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * Domain connection for talking to the dCache admin shell. It negotiates a binary
 * channel using a magic $BINARY$ marker.
 */
public class ShellDomainConnection extends ObjectStreamDomainConnection
{
    private InputStream _inputStream;
    private OutputStream _outputStream;
    private Reader _reader;
    private Writer _writer;

    @Override
    public String getAuthenticatedUser()
    {
        return "Unknown";
    }

    public void setIoStreams(InputStream in, OutputStream out)
    {
        setIoStreams(in, out, null, null);
    }

    public void setIoStreams(InputStream in, OutputStream out,
            Reader reader, Writer writer)
    {
        _inputStream = in;
        _outputStream = out;
        _reader = reader;
        _writer = writer;
    }

    public void go() throws Exception
    {
        try {
            negotiateBinaryMode();
            run(_inputStream, _outputStream);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void negotiateBinaryMode() throws IOException
    {
        BufferedReader reader = new BufferedReader(
                _reader == null
                ? new InputStreamReader(_inputStream)
                : _reader, 1);
        PrintWriter writer = new PrintWriter(_writer == null
                ? new OutputStreamWriter(_outputStream)
                : _writer);
        writer.println("$BINARY$");
        writer.flush();
        String check;
        do {
            check = reader.readLine();
        } while (!check.equals("$BINARY$"));
    }
}
