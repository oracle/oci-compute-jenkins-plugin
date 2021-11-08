package com.oracle.cloud.baremetal.jenkins.client;

import java.net.HttpURLConnection;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.HttpUrlConnectorProvider.ConnectionFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;

import com.oracle.bmc.http.DefaultConfigurator;

import hudson.ProxyConfiguration;

public class HTTPProxyConfigurator extends DefaultConfigurator {
    @Override
    public void setConnectorProvider(ClientBuilder builder) {
        ClientConfig clientConfig = new ClientConfig();

        // OCI API HTTP proxy workaround
        ConnectionFactory connectionFactory = url -> (HttpURLConnection) ProxyConfiguration.open(url);

        // 1) enable workaround for 'patch' requests
        HttpUrlConnectorProvider provider = new HttpUrlConnectorProvider()
                .useSetMethodWorkaround()
                .connectionFactory(connectionFactory);

        clientConfig.connectorProvider(provider);
        builder.withConfig(clientConfig);
    }
}
