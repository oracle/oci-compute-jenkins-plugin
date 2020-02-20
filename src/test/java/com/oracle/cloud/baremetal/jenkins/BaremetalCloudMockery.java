package com.oracle.cloud.baremetal.jenkins;

import java.util.concurrent.atomic.AtomicInteger;

import org.jmock.api.MockObjectNamingScheme;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.CamelCaseNamingScheme;
import org.jmock.lib.concurrent.Synchroniser;

public class BaremetalCloudMockery extends JUnitRuleMockery {
    public BaremetalCloudMockery() {
        setNamingScheme(new IdNamingScheme(CamelCaseNamingScheme.INSTANCE));
        setThreadingPolicy(new Synchroniser());
    }

    private static class IdNamingScheme implements MockObjectNamingScheme {
        private final MockObjectNamingScheme scheme;
        private final AtomicInteger nextId = new AtomicInteger(0);

        IdNamingScheme(MockObjectNamingScheme scheme) {
            this.scheme = scheme;
        }

        @Override
        public String defaultNameFor(Class<?> typeToMock) {
            return scheme.defaultNameFor(typeToMock) + '.' + nextId.getAndIncrement();
        }
    }
}
