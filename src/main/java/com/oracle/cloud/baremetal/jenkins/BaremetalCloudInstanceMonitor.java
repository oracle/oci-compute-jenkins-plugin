package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Node;
import hudson.model.TaskListener;

@Extension
public class BaremetalCloudInstanceMonitor extends AsyncPeriodicWork {
	private static final Logger LOGGER = Logger.getLogger(BaremetalCloudInstanceMonitor.class.getName());

	private static final Long recurrencePeriod = TimeUnit.MINUTES.toMillis(10);

	public BaremetalCloudInstanceMonitor(){
		super("Oracle Oracle Cloud Infrastructure Compute instances monitor");
		LOGGER.log(Level.FINE, "Oracle Cloud Infrastructure Compute check alive period is {0}ms", recurrencePeriod);
	}

	@Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }

    List<Node> getNodes() {
        return JenkinsUtil.getJenkinsInstance().getNodes();
    }

	@Override
	protected void execute(TaskListener listener) {
		for(Node node : getNodes()){
			if(node instanceof BaremetalCloudAgent){
				final BaremetalCloudAgent agent = (BaremetalCloudAgent)node;
				try{
					if(! agent.isAlive()){
						LOGGER.info("Cloud Infrastructure instance is offline: " + agent.getDisplayName());
						agent._terminate(listener);
						LOGGER.info("Cloud Infrastructure instance is terminated: " + agent.getDisplayName());
						removeNode(agent);
					}else{
						LOGGER.info("Cloud Infrastructure instance is online: " + agent.getDisplayName());
					}
				} catch (IOException | InterruptedException | RuntimeException e){
					LOGGER.info("Failed to terminate node : " + agent.getDisplayName());
					removeNode(agent);
				}
			}
		}
	}

	void removeNode(BaremetalCloudAgent agent){
		try{
		    JenkinsUtil.getJenkinsInstance().removeNode(agent);
		} catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to remove node: " + agent.getDisplayName());
        }
	}

}
