package com.oracle.cloud.baremetal.jenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.codec.binary.Base64;

public class SshKeyUtil {
    private static final String SSH_RSA_ALGORITHM_NAME = "ssh-rsa";

    public static String toSshString(RSAPublicKey key) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(out, SSH_RSA_ALGORITHM_NAME.getBytes(StandardCharsets.UTF_8));
            write(out, key.getPublicExponent().toByteArray());
            write(out, key.getModulus().toByteArray());
        } catch (IOException e) {
            // Unreachable: ByteArrayOutputStream does not throw IOException.
            throw new Error(e);
        }

        return SSH_RSA_ALGORITHM_NAME + ' ' + Base64.encodeBase64String(out.toByteArray());
    }

    private static void write(ByteArrayOutputStream out, byte[] b) throws IOException {
        writeMpint(out, b.length);
        out.write(b);
    }

    private static void writeMpint(OutputStream out, int value) throws IOException {
        out.write((value >> 24) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 8) & 0xff);
        out.write(value & 0xff);
    }
}
