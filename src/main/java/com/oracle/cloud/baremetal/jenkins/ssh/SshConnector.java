package com.oracle.cloud.baremetal.jenkins.ssh;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionInfo;
import com.trilead.ssh2.KnownHosts;
import com.trilead.ssh2.ServerHostKeyVerifier;

public class SshConnector {

    private static final Logger LOGGER = Logger.getLogger(SshConnector.class.getName());

    public static Connection createConnection(String host, int port) throws IOException, InterruptedException {
        return new Connection(host, port);
    }

    public static ConnectionInfo connect(Connection conn, int timeoutMillis, String verificationStrategy) throws IOException {
        conn.setTCPNoDelay(true);
        if(verificationStrategy.equals("No Verification")) {
            LOGGER.log(Level.INFO,"No verification strategy chosen.");
            return conn.connect(new NoSshServerHostKeyVerifier(), timeoutMillis, timeoutMillis);
        }
        LOGGER.log(Level.INFO,"Known host verification strategy chosen.");
        return conn.connect(new VerifySshServerHostKeyVerifier(), timeoutMillis, timeoutMillis);
    }

    static class NoSshServerHostKeyVerifier implements ServerHostKeyVerifier {
        @Override
        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) {
            return true;
        }
    }

    static class VerifySshServerHostKeyVerifier implements ServerHostKeyVerifier {
        @Override
        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) throws IOException, InterruptedException {
            KnownHosts knownHosts;
            File knownhostsfile = new File("./known_hosts");
            if (!knownhostsfile.exists()){
                FileCreator fc = new FileCreator();
                fc.createfilename="known_hosts";
                try {
                    fc.createFileOnMaster();
                } catch(InterruptedException e){
                    //log.error("Could not create known hosts file, change verification strategy.)
                }
                knownhostsfile = new File("./known_hosts");
                if(!knownhostsfile.exists()){
                    throw new RuntimeException("No known host file to verify");
                }
            }
            knownHosts = new KnownHosts(knownhostsfile);
            int fingerprintMatch = knownHosts.verifyHostkey(hostname, serverHostKeyAlgorithm, serverHostKey);
            if (fingerprintMatch == 0 ) {
                return true;
            } else if (fingerprintMatch == 1) {
                String[] hostnames = new String[]{hostname};
                KnownHosts.addHostkeyToFile(knownhostsfile,hostnames, serverHostKeyAlgorithm, serverHostKey);
                return true;
            }
            return false;
        }
    }
}
