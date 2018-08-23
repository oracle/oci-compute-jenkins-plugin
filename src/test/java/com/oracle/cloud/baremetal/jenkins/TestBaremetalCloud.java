package com.oracle.cloud.baremetal.jenkins;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.oracle.cloud.baremetal.jenkins.client.BaremetalCloudClient;
import com.oracle.cloud.baremetal.jenkins.retry.Retry;

public class TestBaremetalCloud extends BaremetalCloud{
    public static class Builder {
        String cloudName = "cloudName";
        String fingerprint;
        String apikey;
        String passphrase;
        String tenantId;
        String userId;
        String regionId;
        String instanceCapStr;
        String maxAsyncThreads;
        int nextTemplateId;
        List<? extends BaremetalCloudAgentTemplate> templates;
        BaremetalCloudClient client;
        Clock clock;

        public Builder cloudName(String cloudName) {
            this.cloudName = cloudName;
            return this;
        }

        public Builder fingerprint(String fingerprint){
            this.fingerprint = fingerprint;
            return this;
        }

        public Builder apikey(String apikey){
            this.apikey = apikey;
            return this;
        }

        public Builder passphrase(String passphrase){
            this.passphrase = passphrase;
            return this;
        }

        public Builder tenantId(String tenantId){
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(String userId){
            this.userId = userId;
            return this;
        }

        public Builder regionId(String regionId){
            this.regionId = regionId;
            return this;
        }

        public Builder instanceCapStr(String instanceCapStr) {
            this.instanceCapStr = instanceCapStr;
            return this;
        }
        public Builder maxAsyncThreads(String maxAsyncThreads) {
            this.maxAsyncThreads = maxAsyncThreads;
            return this;
        }
        public Builder nextTemplateId(int nextTemplateId) {
            this.nextTemplateId = nextTemplateId;
            return this;
        }

        public Builder templates(List<? extends BaremetalCloudAgentTemplate> templates) {
            this.templates = templates;
            return this;
        }

        public Builder client(BaremetalCloudClient client) {
            this.client = client;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public TestBaremetalCloud build(){
            return new TestBaremetalCloud(this);
        }
    }

    private final BaremetalCloudClient client;

    public TestBaremetalCloud(){
        this(new Builder());
    }

    public TestBaremetalCloud(Builder builder){
        super(
                builder.cloudName,
                builder.fingerprint,
                builder.passphrase,
                builder.tenantId,
                builder.userId,
                builder.userId,
                builder.regionId,
                builder.instanceCapStr,
                builder.maxAsyncThreads,
                builder.nextTemplateId,
                builder.templates);
        this.client = builder.client;
    }

    @Override
    public BaremetalCloudClient getClient() {
        return Objects.requireNonNull(client, "client");
    }

    @Override
    public String getEncryptedValue(String str) {
        return str;
    }

    @Override
    public String getPlainText(String str) {
        return str;
    }

    @Override
    public Retry<String> getTerminationRetry(Callable<String> task) {
        return new TestRetry<String>(task);
    }
}
