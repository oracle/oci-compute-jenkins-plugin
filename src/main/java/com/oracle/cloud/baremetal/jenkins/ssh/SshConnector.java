package com.oracle.cloud.baremetal.jenkins.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import com.oracle.cloud.baremetal.jenkins.JenkinsUtil;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.HTTPProxyData;
import com.trilead.ssh2.ServerHostKeyVerifier;

import hudson.ProxyConfiguration;

public class SshConnector {

    public static final SshConnector INSTANCE = new SshConnector();

    private static final int PORT = 22;

    public ProxyConfiguration getProxyConfiguration() {
        return JenkinsUtil.getJenkinsInstance().proxy;
    }

    public Connection createConnection(String host) throws IOException, InterruptedException {
        Connection conn = new Connection(host, PORT);
        ProxyConfiguration proxyConfig = getProxyConfiguration();
        Proxy proxy = proxyConfig == null ? Proxy.NO_PROXY : proxyConfig.createProxy(host);
        if (!proxy.equals(Proxy.NO_PROXY) && proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            HTTPProxyData proxyData = null;
            if (null != proxyConfig.getUserName()) {
                proxyData = new HTTPProxyData(address.getHostName(), address.getPort(), proxyConfig.getUserName(), proxyConfig.getPassword());
            } else {
                proxyData = new HTTPProxyData(address.getHostName(), address.getPort());
            }
            conn.setProxyData(proxyData);
        }
        return conn;
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
