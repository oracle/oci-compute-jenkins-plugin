package com.oracle.cloud.baremetal.jenkins;

import java.util.Collection;
import java.util.TreeSet;

import hudson.model.labels.LabelAtom;
import hudson.util.QuotedStringTokenizer;
import jenkins.model.Jenkins;

public class BaremetalCloudTestUtils {

    public static final String INVALID_FINGERPRINT = ":";
    public static final String FINGERPRINT = "0b:79:34:2b:b4:2a:88:2c:98:f7:94:33:a1:78:43:c8";
    public static final String FINGERPRINT2 = " RUN_BREAKGLASS_TOPO_ON_IDM_PROV_ENV";

    public static final String API_KEY = "-----BEGIN RSA PRIVATE KEY-----\n" +
"MIIEpAIBAAKCAQEA9Z4grraJa8DTz6I7p8N6C3n9MXX8BnaGRK5Rp6Pm2VDOQ9Oe\n" +
"Cyef0kAQlSvuVfSXOG5K4gpjXNLQVv3/MLJmUSQ3cEllQF+gt83SPqMz9lJ1rYfA\n" +
"9T8R/qXeOD30KzrsbmpDzzimIWnSOTIm+MMxUzgcmZGLesHf8NCgQr1CDHi1Se/w\n" +
"62j0Z7jE4Gg/+UCwlvDczGvoMT6F9UA8/O7OBSYyjvBWsF+G7BeJdc3rKeKb2/bz\n" +
"6bOppn0mXU7SN3DexaavnUz7H+NwSCjFokPKHVOPZYfWlfGZlDKTnZdvWEs0O1nC\n" +
"I+6d+QnsG3qUNZPRVOCUYppPdloWH2yIzZyAnQIDAQABAoIBAQDAGm9Bdu9AYc7I\n" +
"ZQD3k8IO29iWKMt3Wphle1nOHjld2v+YuRixbMpprUXLBlMg02666jiTVsCkRxZA\n" +
"E+TV95JjAEqD/kO8945CdC5uY0Mu8wurL7RRnIS8gIqvvnUMosdtDJwApTP6ikOs\n" +
"DHfCtRgNGKP10Iog9yXpuaK+0Duqd+5DIlGlCoz1Rt5AO94k8BjBKt8/d4aCGboP\n" +
"QAKbt7jz9TDUOnNWALgEWqVe2uG1kvFMNn59Khw3dWdZbWe8hr3Nq+Caqtw4Rb8J\n" +
"3OBvMYi69w5VMZKljF1miD0m3hHHWf/BrT68XUH59AgMzKAgt2cOQ3zUAKxFpb/+\n" +
"g/Y4YvCBAoGBAPsdIhUwPTDoTQh9fePSCN2s6wYE/6xsU/G5dxcHM1lhV7Mu/upQ\n" +
"E675H3RaHuxNGNkdUN4BdVN8e9p/jTsfphpUpkmMgD+XJZZgPg/00ctnu/htievX\n" +
"jVtnuBuYcRPgpNgktVWhKDjfCqY1vrzV8N8V5GA8pOVxsumshAWfRzjxAoGBAPpl\n" +
"nesyYoqnKqwE+Kbib011gbTivOwU0xrWD4TvBKjK2gfSt3YQG1kv9OhNKAFyfsqR\n" +
"8MHp/57nYeci3TkcGESVjT9tOktEAneHgag+DTgxFVSuvD3+aftLhmgpnMeoYDGU\n" +
"BnXzvK8pXqUdiYOAe01k+rrbtIvWmFebz+gYTWJtAoGAQ8Qq7HjmKoqmL0JedNq7\n" +
"lccSbb9vmAJr7PHWF5rT2q0QU8S8+lK93RxRxr6DVdXAfOcSabcPCIaxjdeaL3ht\n" +
"BPIPJg4Klh6eACTc2sWA0FmgOnylGcZD2YT2BExxR6H6GnJdlUw2ZJKcdxpN7Pv3\n" +
"Uty/ktEK1viGBdWOk03WlKECgYEAwVuXBvhxgAB2wz9ThAt3R6ll7/jnSqcKHb8P\n" +
"XOf2ASIkG8ZsRY9KjySpSnnKWtO/dU4dTKEV8+9ZetNBYciANPYHjOMcEOMDxKmv\n" +
"Rewk6S99+Va1pmnADX3U3LrFhqhPH/new1bkbZ7Up0yX1CRzEuDXfVQLp7CfE4gO\n" +
"lHuvxkECgYAGde4gh9Dh+p/OfhYR2+AGmneWhQR7SqVL2IJPq5m999Wl4e+0T7pp\n" +
"f6D4miMRa9lcN5nP9omL4JkdZDA1FTYjayUUsFWRi6tXzDDk7POv3jVk6cnTGbHb\n" +
"phlBO7YxrmldADsdOyVJdH2u5ayWUFOclj73uhPwPAWnr+LMt5tyzA==\n" +
"-----END RSA PRIVATE KEY-----";
    public static final String TENANT_ID = "ocid1.tenancy.oc1..aaaaaaaashw7efstoxf6v46gevtascttepw3l3d6xxx4gziexn5sxnldyhja";
    public static final String INVALID_API_KEY = "/";
    public static final String INVALID_TENANTID = "/";
    public static final String USER_ID = "ocid1.user.oc1..aaaaaaaanpyla23uskodpmr3snhyxzfslbhwgi6ly7fbacaasrtlaou4lnra";
    public static final String USER_ID2 = "ocid1.user.oc1..aaaaaaaag6d5vhblrvpni5cwnsboum7i2itqw5deo4zrfdac5dqsw3nfkcfq";
    public static final String REGION_ID = "us-phoenix-1";

    /**
     * Similar to {@link hudson.model.Label#parse} but does not use
     * {@link Jenkins#getInstance}.
     */
    public static Collection<LabelAtom> parseLabels(String labels) {
        Collection<LabelAtom> result = new TreeSet<>();
        if (labels != null && !labels.isEmpty()) {
            for (QuotedStringTokenizer tok = new QuotedStringTokenizer(labels); tok.hasMoreTokens();) {
                result.add(new LabelAtom(tok.nextToken()));
            }
        }
        return result;
    }
}
