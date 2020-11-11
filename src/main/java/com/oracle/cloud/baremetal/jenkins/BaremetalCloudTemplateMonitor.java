package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.oracle.bmc.core.model.Image;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClientFactory;
import com.oracle.cloud.baremetal.jenkins.client.SDKBaremetalCloudClientFactory;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.TaskListener;

import hudson.slaves.Cloud;


@Extension
public class BaremetalCloudTemplateMonitor extends AsyncPeriodicWork{
    private static final Logger LOGGER = Logger.getLogger(BaremetalCloudInstanceMonitor.class.getName());
    private static final Long recurrencePeriod = TimeUnit.MINUTES.toMillis(3);

    public BaremetalCloudTemplateMonitor() {
        super("Oracle Oracle Cloud Infrastructure Compute Templates Monitor");
        LOGGER.log(Level.FINE, "Oracle Cloud Infrastructure Compute Templates Monitor check period is {0}ms", recurrencePeriod);
    }

    @Override
    protected void execute(TaskListener taskListener) throws IOException {

        for (Cloud c : JenkinsUtil.getJenkinsInstance().clouds) {
            if (c instanceof BaremetalCloud) {
                BaremetalCloud cloud = (BaremetalCloud) c;
                List<BaremetalCloudAgentTemplate> lstTemplates = new ArrayList<>();
                boolean updateCheck = false;

                for (BaremetalCloudAgentTemplate template: cloud.getTemplates()) {
                    if (template.getAutoImageUpdate()) {
                        String imageId = template.getImage();

                        BaremetalCloudClientFactory factory = SDKBaremetalCloudClientFactory.INSTANCE;
                        BaremetalCloudClient client = factory.createClient(cloud.getCredentialsId(), Integer.parseInt(cloud.getMaxAsyncThreads()));

                        try {
                            List<Image> images = client.getImagesList(template.getImageCompartmentId());

                            for (Image image : images) {
                                if (image.getId().equals(imageId)) {
                                    String imageName = image.getDisplayName();

                                    for (Image image2 : images) {
                                        if (image2.getDisplayName().equals(imageName)
                                                && !image2.getId().equals(imageId)
                                                && image2.getTimeCreated().compareTo(image.getTimeCreated()) > 0) {
                                            LOGGER.log(Level.INFO, "A new version of the image {0} was found. It is used in the template.", imageName);
                                            updateCheck = true;
                                            lstTemplates.add(createNewTemplate(cloud, template, image2.getId()));

                                        }
                                    }

                                }
                            }

                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to get images list", e);
                        }
                    } else {
                        lstTemplates.add(template);
                    }
                }

                //Update a cloud if it has updated templates
                if (updateCheck) {
                    BaremetalCloud newCLoud = new BaremetalCloud(
                            cloud.name,
                            cloud.getCredentialsId(),
                            cloud.getInstanceCapStr(),
                            cloud.getMaxAsyncThreads(),
                            cloud.getNextTemplateId(),
                            lstTemplates);
                    JenkinsUtil.getJenkinsInstance().clouds.replace(cloud,newCLoud);
                    LOGGER.log(Level.INFO, "The cloud {0} was updated by templates monitor because a new image exists in OCI.", cloud.getDisplayName());
                }
            }
        }
    }

    @Override
    public long getRecurrencePeriod() {
        return recurrencePeriod;
    }

    private BaremetalCloudAgentTemplate createNewTemplate(BaremetalCloud cloud, BaremetalCloudAgentTemplate oldTemplate, String newImageId) {
        return new BaremetalCloudAgentTemplate(
                oldTemplate.compartmentId,
                oldTemplate.availableDomain,
                oldTemplate.vcnCompartmentId,
                oldTemplate.vcnId,
                oldTemplate.subnetCompartmentId,
                oldTemplate.subnetId,
                oldTemplate.nsgIds,
                oldTemplate.imageCompartmentId,
                newImageId,
                oldTemplate.shape,
                oldTemplate.sshCredentialsId,
                oldTemplate.description,
                oldTemplate.remoteFS,
                oldTemplate.assignPublicIP,
                oldTemplate.usePublicIP,
                oldTemplate.numExecutors,
                oldTemplate.mode,
                oldTemplate.labelString,
                oldTemplate.idleTerminationMinutes,
                oldTemplate.templateId,
                oldTemplate.initScript,
                oldTemplate.sshConnectTimeoutSeconds,
                oldTemplate.startTimeoutSeconds,
                oldTemplate.initScriptTimeoutSeconds,
                oldTemplate.instanceCap,
                oldTemplate.numberOfOcpus,
                oldTemplate.getAutoImageUpdate(),
                oldTemplate.getStopOnIdle(),
                oldTemplate.getTags(),
                oldTemplate.getInstanceNamePrefix()
        );

    }
}
