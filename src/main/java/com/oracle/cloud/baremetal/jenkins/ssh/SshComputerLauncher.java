package com.oracle.cloud.baremetal.jenkins.ssh;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.oracle.cloud.baremetal.jenkins.JenkinsUtil;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.Channel.Listener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

public class SshComputerLauncher extends ComputerLauncher {
    private static final Logger LOGGER = Logger.getLogger(SshComputerLauncher.class.getName());

    private static final int RETRY_TIMES = 1;

    private final String host;
    private final int connectTimeoutMillis;
    private final String privateKey;
    private final String initScript;
    private final String remoteAdmin;
    private final int initScriptTimeoutSeconds;


    public SshComputerLauncher(
            final String host,
            final int connectTimeoutMillis,
            final String privateKey,
            final String initScript,
            final int initScriptTimeoutSeconds,
            final String remoteAdmin) {
        this.host = host;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.privateKey = privateKey;
        this.initScript = initScript;
        this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
        this.remoteAdmin = remoteAdmin;
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener) throws IOException, InterruptedException {
    	LOGGER.info("Launcher agent on host: " + this.host);
    	final Connection conn;
        Connection retryConn = null;
        PrintStream logger = listener.getLogger();
        try {
            SshConnector sshConnector = SshConnector.INSTANCE;

            String sshUser = "opc";
            if(remoteAdmin != null && !remoteAdmin.trim().isEmpty()){
                sshUser = remoteAdmin;
            }
            LOGGER.info("The ssh user name is: " + sshUser);

            int i = 0;
            do {
                retryConn = sshConnector.createConnection(host);
                try {
                    sshConnector.connect(retryConn, connectTimeoutMillis);
                    break;
                } catch (Exception e) {
                    retryConn.close();

                    if (i++ == RETRY_TIMES) {
                        throw new IOException("SSH launch failed at connecting to host: " + this.host, e);
                    }
                    LOGGER.log(Level.FINER, "Ignoring connection exception when executing ssh launch on host: " + this.host, e);
                }
            } while (true);

            conn = retryConn;

            i = 0;
            do {
               try {
                   if (conn.authenticateWithPublicKey(remoteAdmin, this.privateKey.toCharArray(), null))
                       break;
               } catch (IOException e) {
                   if (i++ == RETRY_TIMES) {
                       conn.close();
                       throw new IOException("SSH launch failed at authenticating to host: " + this.host, e);
                   }
                   LOGGER.log(Level.FINER, "Ignoring connection exception when authenticating with public key during ssh launch on host: " + this.host, e);
               }
            } while (true);

            SCPClient scp = conn.createSCPClient();

            Slave agent = computer.getNode();
            String remoteFS = agent == null ? null : agent.getRemoteFS();
            if (remoteFS == null || remoteFS.trim().isEmpty()) {
                remoteFS = ".";
            }

            conn.exec("mkdir -p " + remoteFS, logger);

            if (initScript != null && initScript.trim().length() > 0
                    && conn.exec("test -e ~/.hudson-run-init", logger) != 0) {
                scp.put(initScript.getBytes("UTF-8"), "init.sh", remoteFS, "0700");
                Session sess = conn.openSession();
                sess.requestDumbPTY();
                sess.execCommand("/bin/bash" + " " + remoteFS + "/init.sh");

                sess.getStdin().close();
                sess.getStderr().close();
                IOUtils.copy(sess.getStdout(), logger);

                int exitStatus = waitCompletion(sess);
                if (exitStatus != 0) {
                    LOGGER.warning("init script failed: exit code=" + exitStatus);
                    sess.close();
                    conn.close();
                    return;
                }
                sess.close();

                sess = conn.openSession();
                sess.requestDumbPTY(); // so that the remote side bundles stdout
                                       // and stderr
                sess.execCommand("/bin/bash touch ~/.hudson-run-init");
                sess.close();
            }

            scp.put(JenkinsUtil.getJenkinsInstance().getJnlpJars("slave.jar").readFully(), "slave.jar", remoteFS);

            String launchString = "java -jar " + remoteFS + "/slave.jar";

            LOGGER.info("Launching slave agent (via Trilead SSH2 Connection): " + launchString);
            final Session sess = conn.openSession();
            sess.execCommand(launchString);
            computer.setChannel(sess.getStdout(), sess.getStdin(), logger, new Listener() {
                @Override
                public void onClosed(Channel channel, IOException cause) {
                    sess.close();
                    conn.close();
                }
            });
        } finally {

        }
    }

    private int waitCompletion(Session session) throws InterruptedException {
        LOGGER.info("Timeout around for init script complete is " + initScriptTimeoutSeconds);
        for (int i = 0; i < initScriptTimeoutSeconds; i++) {
            Integer r = session.getExitStatus();
            if (r != null)
                return r;
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        }
        return -1;
    }
}

