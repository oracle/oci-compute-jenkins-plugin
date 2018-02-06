package com.oracle.cloud.baremetal.jenkins.ssh;

import java.io.IOException;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ServerHostKeyVerifier;

public class SshConnector {

    public static final SshConnector INSTANCE = new SshConnector();

    private static final int PORT = 22;

    public Connection createConnection(String host) throws IOException, InterruptedException {
        return new Connection(host, PORT);
    }

    public void connect(Connection conn, int timeoutMillis) throws IOException {
        conn.connect(new SshServerHostKeyVerifier(), timeoutMillis, timeoutMillis);
    }

    static class SshServerHostKeyVerifier implements ServerHostKeyVerifier {
        @Override
        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) {
            return true;
        }
    }
}
