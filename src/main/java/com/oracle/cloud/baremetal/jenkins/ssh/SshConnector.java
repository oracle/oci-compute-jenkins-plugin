package com.oracle.cloud.baremetal.jenkins.ssh;

import java.io.IOException;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionInfo;
import com.trilead.ssh2.ServerHostKeyVerifier;

public class SshConnector {
    public static Connection createConnection(String host, int port) throws IOException, InterruptedException {
        return new Connection(host, port);
    }

    public static ConnectionInfo connect(Connection conn, int timeoutMillis) throws IOException {
        conn.setTCPNoDelay(true);
        return conn.connect(new SshServerHostKeyVerifier(), timeoutMillis, timeoutMillis);
    }

    static class SshServerHostKeyVerifier implements ServerHostKeyVerifier {
        @Override
        public boolean verifyServerHostKey(String hostname, int port, String serverHostKeyAlgorithm, byte[] serverHostKey) {
            return true;
        }
    }
}
