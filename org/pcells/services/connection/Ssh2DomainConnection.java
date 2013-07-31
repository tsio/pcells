package org.pcells.services.connection;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 */
public class Ssh2DomainConnection
        extends ObjectStreamDomainConnection
{
    private String _hostname;
    private int _portnumber;
    private String _password;
    private String _loginName = "Unknown";
    private File _identityFile;

    public Ssh2DomainConnection(String hostname, int portnumber)
    {
        _hostname = hostname;
        _portnumber = portnumber;
        System.out.println(this.getClass().getName() + " loaded by : " + this.getClass().getClassLoader().getClass().getName());
    }

    @Override
    public String getAuthenticatedUser()
    {
        return _loginName;
    }

    private void authenticate(ClientSession session) throws InterruptedException, IOException
    {
        AuthFuture authFuture;
        /* The following didn't work for me as the FileKeyPairProvider was unable to
         * parse my id_rsa file.
         *
        if (_identityFile != null) {
            FileKeyPairProvider provider = new FileKeyPairProvider(
                    new String[]{_identityFile.getPath()},
                    new PasswordFinder()
                    {
                        @Override
                        public char[] getPassword()
                        {
                            return _password.toCharArray();
                        }
                    });
            for (KeyPair pair: provider.loadKeys()) {
                authFuture = session.authPublicKey(_loginName, pair);
                authFuture.await();
                if (authFuture.isSuccess()) {
                    return;
                }
            }
        }
        */
        authFuture = session.authPassword(_loginName, _password);
        authFuture.await();
        if (authFuture.isSuccess()) {
            return;
        }
        Throwable exception = authFuture.getException();
        if (exception != null) {
            throw new IOException(exception.getMessage(), exception);
        }
        throw new IOException("Authentication failed");
    }

    public void go() throws Exception
    {
        System.out.println("Running Ssh2 GO");
        SshClient ssh2Client = SshClient.setUpDefaultClient();
        ssh2Client.start();
        try {
            ConnectFuture connectFuture = ssh2Client.connect(_hostname, _portnumber);
            connectFuture.await();
            if (!connectFuture.isConnected()) {
                Throwable exception = connectFuture.getException();
                if (exception != null) {
                    throw new IOException(exception.getMessage(), exception);
                }
                throw new IOException("Connection failed");
            }
            ClientSession session = connectFuture.getSession();
            authenticate(session);
            ClientChannel channel = session.createSubsystemChannel("pcells");
            PipedOutputStream guiOutputStream = new PipedOutputStream();
            PipedInputStream clientInputStream = new PipedInputStream();
            guiOutputStream.connect(clientInputStream);
            PipedOutputStream clientOutputStream = new PipedOutputStream();
            PipedInputStream guiInputStream = new PipedInputStream();
            clientOutputStream.connect(guiInputStream);
            channel.setIn(clientInputStream);
            channel.setOut(clientOutputStream);
            channel.setErr(
                    new OutputStream()
                    {
                        @Override
                        public void write(int b) throws IOException
                        {
                            throw new UnsupportedOperationException();
                        }
                    });
            channel.open().await();

            try {
                run(guiInputStream, guiOutputStream);
            } finally {
                channel.close(true).await();
                session.close(true);
            }
        } finally {
            ssh2Client.stop();
        }
    }

    public void setLoginName(String name) {
        _loginName = name;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public void setIdentityFile(File identityFile) throws Exception {
        _identityFile = identityFile;
    }
}
