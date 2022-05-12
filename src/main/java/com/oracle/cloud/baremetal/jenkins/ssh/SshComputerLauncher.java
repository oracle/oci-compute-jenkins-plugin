package com.oracle.cloud.baremetal.jenkins.ssh;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.oracle.cloud.baremetal.jenkins.BaremetalCloud;
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
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

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

    private final String jenkinsAgentUser;
    private final String initScript;
    private final int initScriptTimeoutSeconds;
    private transient SSHUserPrivateKey sshCredentials;

    public SshComputerLauncher(
            final String host,
            final int connectTimeoutMillis,
	    final String jenkinsAgentUser,
            final String initScript,
            final int initScriptTimeoutSeconds,
            final String sshCredentialsId) {
        this(host, connectTimeoutMillis, jenkinsAgentUser, initScript,
	     initScriptTimeoutSeconds, sshCredentialsId, DEFAULT_SSH_PORT);
    }

    public SshComputerLauncher(
            final String host,
            final int connectTimeoutMillis,
            final String jenkinsAgentUser,
            final String initScript,
            final int initScriptTimeoutSeconds,
            final String sshCredentialsId,
            final int sshPort) {
        this.sshCredentials = (SSHUserPrivateKey) BaremetalCloud.matchCredentials(SSHUserPrivateKey.class, sshCredentialsId);
        this.host = host;
        this.connectTimeoutMillis = connectTimeoutMillis;
        if (sshCredentials != null) {
            this.privateKey = sshCredentials.getPrivateKey();
        } else {
            this.privateKey = DEFAULT_SSH_PUBLIC_KEY;
        }
        this.jenkinsAgentUser = jenkinsAgentUser;
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
            listener.fatalError("SSH Agent launch failed on: " + sshUser + "@" + host + ":" + sshPort);
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
        listener.getLogger().println("Verifying that Java is installed");
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

        if (initScript == null || initScript.trim().length() <= 0) {
            listener.getLogger().println("No init script to copy to remote agent");
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
	    final String initIndicatorFile = "~/.hudson-run-init";
	    final String initCommand =
                "/bin/bash -c \"if [[ -e " + initIndicatorFile + " ]]; then" +
                        "  echo 'Agent already initialized '" + initIndicatorFile +" exists;" +
                        "else" +
                        "  echo 'Running init script on agent';" +
                        "  /bin/bash " + remoteDirectory + "/init.sh;" +
                        "  echo Creating " + initIndicatorFile + " on agent;" +
                        "  touch " + initIndicatorFile + ";" +
                        "fi\"";
            initSession.execCommand(initCommand);

            IOUtils.copy(initSession.getStdout(), listener.getLogger());
            IOUtils.copy(initSession.getStderr(), listener.getLogger());
            initSession.getStdin().close();
            initSession.getStderr().close();

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
    }


    private void copyAgentJar(Connection connection, String remoteDirectory, final TaskListener listener)
            throws IOException {

	// Delete slave.jar in case it already exists and is owned by a different
	// user, which could happen if Jenkins Agent User is set.
	//
	String deleteString = "sudo rm -f " + remoteDirectory + "/slave.jar";
        listener.getLogger().println("Deleting remote slave.jar if it exists prior to copy ["
	             + deleteString + "]");
        Session deleteSession = null;
        try {
            deleteSession = connection.openSession();
            deleteSession.requestDumbPTY();
            deleteSession.execCommand(deleteString);
            deleteSession.getStdin().close();
            IOUtils.copy(deleteSession.getStderr(), listener.getLogger());
            IOUtils.copy(deleteSession.getStdout(), listener.getLogger());
        } catch (IOException e) {
            listener.fatalError("Failed trying to delete slave.jar on remote agent ["
	             + e.getMessage() +"] command=[" + deleteString + "]");
            throw e;
        } finally {
            if (deleteSession != null) {
                deleteSession.close();
            }
        }

        listener.getLogger().println("Copying slave.jar to remote agent using scp");

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

        String jarfile = remoteDirectory + "/slave.jar";
        String launchString = "java -jar " + jarfile;
        if (jenkinsAgentUser == null || jenkinsAgentUser.trim().isEmpty()) {
            listener.getLogger().println("Jenkins Agent User is empty, default opc.");
        } else {
            launchString = "sudo chown " + jenkinsAgentUser + " " + jarfile +
	                  " && sudo -u " + jenkinsAgentUser + " " + launchString;
        }

        listener.getLogger().println("Launching Agent (via Trilead SSH2 Connection): "
	          + launchString);

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
            listener.fatalError("Failed to launch Agent");
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
