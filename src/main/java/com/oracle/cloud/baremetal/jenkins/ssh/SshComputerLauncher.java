package com.oracle.cloud.baremetal.jenkins.ssh;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;

import com.oracle.cloud.baremetal.jenkins.JenkinsUtil;
import com.oracle.cloud.baremetal.jenkins.retry.LinearRetry;
import com.trilead.ssh2.ChannelCondition;
import com.trilead.ssh2.Connection;
import com.trilead.ssh2.ConnectionInfo;
import com.trilead.ssh2.SCPClient;
import com.trilead.ssh2.Session;

import hudson.model.Slave;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.Channel.Listener;
import hudson.security.ACL;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import java.util.Collections;
import jenkins.model.Jenkins;

public class SshComputerLauncher extends ComputerLauncher {
    private static final Logger LOGGER = Logger.getLogger(SshComputerLauncher.class.getName());

    public static final String DEFAULT_SSH_USER = "opc";
    public static final int DEFAULT_SSH_PORT = 22;
    public static final String DEFAULT_SSH_PUBLIC_KEY = " ";

    private final String host;
    private final int sshPort;
    private final String sshUser;

    private final int connectTimeoutMillis;
    private final String privateKey;

    private final String initScript;
    private final int initScriptTimeoutSeconds;
    private transient SSHUserPrivateKey sshCredentials;

    public SshComputerLauncher(
            final String host,
            final int connectTimeoutMillis,
            final String initScript,
            final int initScriptTimeoutSeconds,
            final String sshCredentialsId) {
        this(host, connectTimeoutMillis, initScript, initScriptTimeoutSeconds, sshCredentialsId, DEFAULT_SSH_PORT);
    }

    public SshComputerLauncher(
            final String host,
            final int connectTimeoutMillis,
            final String initScript,
            final int initScriptTimeoutSeconds,
            final String sshCredentialsId,
            final int sshPort) {
        this.sshCredentials = CredentialsMatchers.firstOrNull(
            CredentialsProvider.lookupCredentials(SSHUserPrivateKey.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()),
            CredentialsMatchers.withId(sshCredentialsId));
        this.host = host;
        this.connectTimeoutMillis = connectTimeoutMillis;
        if (sshCredentials != null) {
            this.privateKey = sshCredentials.getPrivateKey();
        } else {
            this.privateKey = DEFAULT_SSH_PUBLIC_KEY;
        }
        this.initScript = initScript;
        this.initScriptTimeoutSeconds = initScriptTimeoutSeconds;
        if (sshCredentials != null) {
            this.sshUser = sshCredentials.getUsername();
        } else {
            this.sshUser = DEFAULT_SSH_USER;
        }
        this.sshPort = sshPort;
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener) throws IOException,
            InterruptedException {

        Connection connection = null;
        try {
            connection = connect(listener);
            authenticate(connection, listener);

            String workingDirectory = getRemoteWorkingDirectory(computer);
            createRemoteDirectory(connection, workingDirectory, listener);
            runInitScript(connection, workingDirectory, listener);

            ensureJavaInstalled(connection, listener);
            copyAgentJar(connection, workingDirectory, listener);
            launchAgent(connection, workingDirectory, computer, listener);
        } catch (IOException | InterruptedException e) {
            tearDownConnection(connection, listener);
            listener.fatalError("SSH Agen launch failed on: " + sshUser + "@" + host + ":" + sshPort);
            throw e;
        }
    }

    private Connection connect(final TaskListener listener) throws IOException, InterruptedException {
        final String uri = sshUser + "@" + host + ":" + sshPort;

        listener.getLogger().println("Connecting to ssh: " + uri);
        try {
            Connection connection = SshConnector.createConnection(host, sshPort);
            new LinearRetry<ConnectionInfo>(() ->
                SshConnector.connect(connection, connectTimeoutMillis)).run();

            return connection;
        } catch (Exception e) {
            listener.fatalError("Failed to connect to ssh: " + uri);
            throw new IOException(e);
        }
    }

    private void authenticate(Connection connection, final TaskListener listener) throws IOException {
        listener.getLogger().println("Authenticating with private key");
        try {
            boolean authenticated = connection.authenticateWithPublicKey(sshUser, this.privateKey.toCharArray(), null);
            if (!authenticated) {
                throw new IOException("SSH launch failed at authenticating to host: " + this.host);
            }
        } catch (IOException e) {
            listener.fatalError("Failed to authenticate");
            throw e;
        }
    }

    private void ensureJavaInstalled(final Connection connection, final TaskListener listener) throws IOException, InterruptedException {
        listener.getLogger().println("Veryfing that Java is installed");
        int ret = connection.exec("java -fullversion", listener.getLogger());

        if (ret != 0) {
            listener.fatalError("Agent does not have java installed");
            throw new IOException("Agent does not have java installed: " + this.host);
        }
    }

    private String getRemoteWorkingDirectory(final SlaveComputer computer) {
        Slave agent = computer.getNode();

        if (agent == null || agent.getRemoteFS().trim().isEmpty()) {
            return ".";
        }

        return agent.getRemoteFS();
    }

    private void createRemoteDirectory(Connection connection, String remoteDirectory, final TaskListener listener)
            throws IOException,
            InterruptedException {
        try {
            connection.exec("mkdir -p \"" + remoteDirectory + "\"", listener.getLogger());
        } catch (IOException | InterruptedException e) {
            listener.fatalError("Failed to create remote working directory: " + remoteDirectory);
            throw e;
        }
    }

    private void runInitScript(Connection connection, String remoteDirectory, final TaskListener listener)
            throws InterruptedException,
            IOException {
        final String initIndicationFile = "~/.hudson-run-init";

        if (initScript == null || initScript.trim().length() <= 0) {
            listener.getLogger().println("No init script to copy to remote agent");
            return;
        }
        if (connection.exec("test -e \"" + initIndicationFile + "\"", listener.getLogger()) == 0) {
            listener.getLogger().println("Init script previously executed on remote agent (\"" + initIndicationFile + "\" exists) - skipping");
            return;
        }

        listener.getLogger().println("Copying init script to remote agent using scp");
        try {
            SCPClient scp = connection.createSCPClient();
            scp.put(initScript.getBytes("UTF-8"), "init.sh", remoteDirectory, "0700");
        } catch (IOException e) {
            listener.fatalError("Failed to copy init script");
            throw e;
        }

        listener.getLogger().println("Running init script on remote agent");
        Session initSession = null;
        try {
            initSession = connection.openSession();
            initSession.requestDumbPTY();
            initSession.execCommand("/bin/bash " + remoteDirectory + "/init.sh");

            initSession.getStdin().close();
            initSession.getStderr().close();
            IOUtils.copy(initSession.getStdout(), listener.getLogger());

            int exitStatus = waitCompletion(initSession);
            if (exitStatus != 0) {
                String msg = "Init script on " + this.host + " finished with non-zero exit status: " + exitStatus;
                listener.getLogger().println(msg);
                throw new IOException(msg);
            }
        } catch (IOException e) {
            listener.fatalError("Failed to execute init script on remote agent");
            throw e;
        } finally {
            if (initSession != null) {
                initSession.close();
            }
        }

        Session touchSession = null;
        try {
            touchSession = connection.openSession();
            touchSession.requestDumbPTY();
            touchSession.execCommand("touch \"" + initIndicationFile + "\"");
        } finally {
            if (touchSession != null) {
                touchSession.close();
            }
        }
    }


    private void copyAgentJar(Connection connection, String remoteDirectory, final TaskListener listener)
            throws IOException {
        listener.getLogger().println("Copying the slave.jar to remote agent using scp");

        try {
            SCPClient scp = connection.createSCPClient();
            scp.put(JenkinsUtil.getJenkinsInstance().getJnlpJars("slave.jar").readFully(),
                    "slave.jar",
                    remoteDirectory);
        } catch (IOException e) {
            listener.fatalError("Failed to copy slave.jar");
            throw e;
        }
    }

    private void launchAgent(Connection connection,
            String remoteDirectory,
            final SlaveComputer computer,
            final TaskListener listener)
            throws IOException,
            InterruptedException {
        String launchString = "java -jar " + remoteDirectory + "/slave.jar";
        listener.getLogger().println("Launching Agent (via Trilead SSH2 Connection): " + launchString);

        Session session = connection.openSession();
        try {
            session.execCommand(launchString);
            computer.setChannel(session.getStdout(), session.getStdin(), listener.getLogger(), new Listener() {
                        @Override
                        public void onClosed(Channel channel, IOException cause) {
                            tearDownSession(session, listener);
                            tearDownConnection(connection, listener);
                        }
                    });
        } catch (IOException | InterruptedException e) {
            tearDownSession(session, listener);
            listener.fatalError("Failed to launching Agent");
            throw e;
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

    private void tearDownSession(Session session, final TaskListener listener) {
        if (session == null) {
            return;
        }

        reportSeesionTerminationReason(session, listener);

        listener.getLogger().println("Closing SSH Session to: " + this.host);
        try {
            session.getStdout().close();
            session.close();
        } catch (Exception e) {
            e.printStackTrace(listener.error("Error while closing SSH Session to: " + this.host));
        }
    }

    private void reportSeesionTerminationReason(Session session, final TaskListener listener) {
        try {
            // Wait 2s for delayed messages before closing the Session
            session.waitForCondition(ChannelCondition.EXIT_STATUS | ChannelCondition.EXIT_SIGNAL, 2000);
        } catch (InterruptedException e) {
            // Ignore
        }

        Integer exitCode = session.getExitStatus();
        if (exitCode != null) {
            listener.getLogger().println("Remote Agent has terminated with exit code: " + exitCode);
        }

        String exitSignal = session.getExitSignal();
        if (exitSignal != null) {
            listener.getLogger().println("SSH Sesson has terminated with exit signal: " + exitCode);
        }
    }

    private void tearDownConnection(Connection connection, final TaskListener listener) {
        if (connection == null) {
            return;
        }

        reportConnectionTerminationReason(connection, listener);

        listener.getLogger().println("Closing SSH Connection to");
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace(listener.error("Error while closing SSH Connection to: " + this.host));
        }
    }

    private void reportConnectionTerminationReason(Connection connection, TaskListener listener) {
        Throwable cause = connection.getReasonClosedCause();
        if (cause != null) {
            cause.printStackTrace(listener.error("Connection to SSH server lost"));
        }
    }
}
