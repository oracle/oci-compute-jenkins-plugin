package com.oracle.cloud.baremetal.jenkins.credentials;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.remoting.RoleChecker;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class OciConfigWriter implements Serializable {
    public static final String ENV_USER_ID = "OCI_USER_ID";
    public static final String ENV_FINGERPRINT = "OCI_FINGERPRINT";
    public static final String ENV_TENANT_ID = "OCI_TENANT_ID";
    public static final String ENV_REGION_ID = "OCI_REGION_ID";
    public static final String ENV_KEY_FILE = "OCI_KEY_FILE";
    public static final String ENV_CONFIG_FILE = "OCI_CONFIG_FILE";

    private final String user;
    private final String fingerprint;
    private final String tenancy;
    private final String region;
    private final String keyFileContents;

    public OciConfigWriter(String user, String fingerprint, String tenancy, String region, String keyFileContents) {
        this.user = user;
        this.fingerprint = fingerprint;
        this.tenancy = tenancy;
        this.region = region;
        this.keyFileContents = keyFileContents;
    }

    public String getUser() {
        return user;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getTenancy() {
        return tenancy;
    }

    public String getRegion() {
        return region;
    }

    public String getKeyFileContents() {
        return keyFileContents;
    }

    public static class Callable implements FileCallable<Map<String, String>> {
        private final OciConfigWriter writer;

        public Callable(final OciConfigWriter writer) {
            this.writer = writer;
        }

        @Override
        public Map<String, String> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            return writer.createEnvironment(f);
        }

        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {}
    }

    public Callable asCallable() {
        return new Callable(this);
    }

    // Wrap this in its own function so we don't get too nested..
    private String getConfigText(final Path keyFile) throws IOException {
        try(final InputStream stream = getClass().getResourceAsStream("config.template")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("%USER%", user)
                    .replaceAll("%FINGERPRINT%", fingerprint)
                    .replaceAll("%TENANCY%", tenancy)
                    .replaceAll("%REGION%", region)
                    .replaceAll("%KEY_FILE_PATH%", keyFile.toString());
        }
    }

    public Map<String, String> createEnvironment(final File dir) throws IOException {
        final Path keyFile = dir.toPath().resolve("oci.pem");
        final Path config = dir.toPath().resolve("oci.config");
        final String text = getConfigText(keyFile);
        try(final BufferedWriter writer =
                    Files.newBufferedWriter(
                            keyFile,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(keyFileContents);
        }
        try(final BufferedWriter writer =
                    Files.newBufferedWriter(
                            config,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(text);
        }
        Map<String, String> env = new HashMap<>();

        env.put(ENV_USER_ID, getUser());
        env.put(ENV_FINGERPRINT, getFingerprint());
        env.put(ENV_TENANT_ID, getTenancy());
        env.put(ENV_REGION_ID, getRegion());
        env.put(ENV_KEY_FILE, keyFile.toString());
        env.put(ENV_CONFIG_FILE, config.toString());

        return env;
    }
}
