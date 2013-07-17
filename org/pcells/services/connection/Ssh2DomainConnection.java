// $Id: Ssh1DomainConnection.java,v 1.5 2007/02/15 08:18:12 cvs Exp $
//
package org.pcells.services.connection;
//
import dmg.protocols.ssh.*;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.buffer.UnboundedFifoBuffer;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;

/**
 */
public class Ssh2DomainConnection
        extends DomainConnectionAdapter
        implements SshClientAuthentication {

    private String _hostname = null;
    private int _portnumber = 0;
    private Socket _socket = null;
    private ClientSession _session = null;
    public SshAuthRsa _rsaAuth = null;
    public String _password = null;
    public String _loginName = "Unknown";

    public Ssh2DomainConnection(String hostname, int portnumber) {
        _hostname = hostname;
        _portnumber = portnumber;

        System.out.println(this.getClass().getName() + " loadeded by : " + this.getClass().getClassLoader().getClass().getName());

    }

    public void go() throws Exception {
        System.out.println("Running Ssh2 GO");
        SshClient ssh2Client = SshClient.setUpDefaultClient();
        System.out.println("Initialized Ssh2Client");
        ssh2Client.start();
        try {
            System.out.println("Ssh2Client started. Creating Session");
            ConnectFuture connectFuture = ssh2Client.connect(_hostname, _portnumber);
            connectFuture.awaitUninterruptibly();
            System.out.println("Connection Ssh2 successfull?: " + connectFuture.isConnected());
            if (connectFuture.getSession() != null) {
                _session = connectFuture.getSession();
            } else {
                System.out.println("The session does not exist.");
            }
            System.out.println("Ssh2ClientSession created: " + _session.toString());
            int ret = ClientSession.WAIT_AUTH;
            AuthFuture authFuture = null;
            while ((ret & ClientSession.WAIT_AUTH) != 0) {
                authFuture = _session.authPassword(_loginName, _password);
                ret = _session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
            }
            System.out.println("Ssh2 AuthFuture: " + authFuture);
            if (authFuture != null) {
                if (authFuture.isSuccess()) {
                    ClientChannel channel = _session.createShellChannel();
                    // Connecting streams of sshClient and DomainConnectionAdapter
                    PipedOutputStream guiOutputStream = new PipedOutputStream();
                    PipedInputStream clientInputStream = new PipedInputStream();
                    guiOutputStream.connect(clientInputStream);
                    PipedOutputStream clientOutputStream = new PipedOutputStream();
                    PipedInputStream guiInputStream = new PipedInputStream();
                    clientOutputStream.connect(guiInputStream);
                    channel.setIn(clientInputStream);
                    channel.setOut(clientOutputStream);
                    channel.setErr(new OutputStream() {

                        @Override
                        public void write(int b) throws IOException {
                            throw new UnsupportedOperationException("Not supported yet.");
                        }
                    });
                    channel.open().await();
                    setIoStreams(
                            guiInputStream,
                            guiOutputStream,
                            new InputStreamReader(guiInputStream),
                            new OutputStreamWriter(guiOutputStream));
                    super.go();
                    int channelReturn = channel.waitFor(ClientChannel.CLOSED, 0);
                    if (channelReturn == ClientChannel.EXIT_SIGNAL) _session.close(true);
                }
            } else {
            }
        } catch (Exception e) {
            System.out.println("Ssh2 Exception caught: " + e.toString());
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

        InputStream in = new FileInputStream(identityFile);
        SshRsaKey key = new SshRsaKey(in);
        try {
            in.close();
        } catch (Exception ee) {
        }

        _rsaAuth = new SshAuthRsa(key);

    }
    ////////////////////////////////////////////////////////////////////////////////////////
    //
    //   Client Authentication interface
    //
    private int _requestCounter = 0;

    public boolean isHostKey(InetAddress host, SshRsaKey keyModulus) {


        //      System.out.println( "Host key Fingerprint\n   -->"+
        //                      keyModulus.getFingerPrint()+"<--\n"   ) ;

        //     NOTE : this is correctly done in : import dmg.cells.applets.login.SshLoginPanel

        return true;
    }

    public String getUser() {
        _requestCounter = 0;
        return _loginName;
    }

    public SshSharedKey getSharedKey(InetAddress host) {
        return null;
    }

    public SshAuthMethod getAuthMethod() {

        SshAuthMethod result = null;
        if (_requestCounter++ == 0) {
            if (_rsaAuth == null) {
                result = new SshAuthPassword(_password);
            } else {
                result = _rsaAuth;
            }
        } else if (_requestCounter++ <= 2) {
            result = new SshAuthPassword(_password);
        } else {
            result = null;
        }
//       System.out.println("getAuthMethod("+_requestCounter+") "+result) ;
        return result;
    }

    public static void main(String[] args) throws Exception {
//        if (args.length < 2) {
//
//            System.err.println("Usage : <hostname> <portNumber>");
//            System.exit(4);
//        }
//        String hostname = args[0];
//        int portnumber = Integer.parseInt(args[1]);
        String hostname = "vm-dcache-deploy5.desy.de";
        int portnumber = 22224;
        try {
            System.out.println("Pinging host:" + InetAddress.getByName(hostname).isReachable(1000));
            System.out.println("Host is reachable");
        } catch (Exception e) {
            System.out.println("Host is not reachable");
            e.printStackTrace();
        }
        Ssh2DomainConnection connection = new Ssh2DomainConnection(hostname, portnumber);
        System.out.println("Starting Test");
        RunConnection runCon = connection.test();
        new Thread(runCon).start();

    }

    private class RunConnection
            implements Runnable, DomainConnectionListener, DomainEventListener {

        public RunConnection() throws Exception {
            System.out.println("class runConnection init");
            addDomainEventListener(this);
            System.out.println("Event listener added");
            setLoginName("admin");
            System.out.println("LoginName set");
//            setIdentityFile(new File("/Users/chris/.ssh/identity"));
            setPassword("dickerelch");
            System.out.println("Password set");
        }

        @Override
        public void run() {
            try {
                System.out.println("started Thread run");
                go();
//                connectionOpened(new Ssh2DomainConnection(_hostname, _portnumber));
                System.out.println("After go() call");
            } catch (Exception ee) {
                System.out.println("RunConnection got : " + ee);
                ee.printStackTrace();
            }
        }

        public void domainAnswerArrived(Object obj, int id) {
            System.out.println("Answer : " + obj);
            if (id == 54) {
                try {
                    sendObject("logoff", this, 55);
                } catch (Exception ee) {
                    System.out.println("Exception in sendObject" + ee);
                }
            }
        }

        public void connectionOpened(DomainConnection connection) {
            System.out.println("DomainConnection : connectionOpened");
            try {
                sendObject("System", "ps -f", this, 54);
            } catch (Exception ee) {
                System.out.println("Exception in sendObject" + ee);
            }
        }

        public void connectionClosed(DomainConnection connection) {
            System.out.println("DomainConnection : connectionClosed");
        }

        public void connectionOutOfBand(DomainConnection connection,
                Object subject) {
            System.out.println("DomainConnection : connectionOutOfBand");
        }
    }

    public RunConnection test() throws Exception {
        System.out.println("Starting Test method");
        return new Ssh2DomainConnection.RunConnection();
    }
}
