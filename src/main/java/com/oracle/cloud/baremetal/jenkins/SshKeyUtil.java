package com.oracle.cloud.baremetal.jenkins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;


import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.NotImplementedException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

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
    private static byte[] getSshPublicKeyBody(RSAPublicKey rsaPubKey) throws IOException {
        byte[] algorithmName = "ssh-rsa".getBytes("UTF-8");
        byte[] algorithmNameLength = ByteBuffer.allocate(4).putInt(algorithmName.length).array();
        byte[] e = rsaPubKey.getPublicExponent().toByteArray(); // Usually 65,537
        byte[] eLength = ByteBuffer.allocate(4).putInt(e.length).array();
        byte[] m = rsaPubKey.getModulus().toByteArray();
        byte[] mLength = ByteBuffer.allocate(4).putInt(m.length).array();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(algorithmNameLength);
        os.write(algorithmName);
        os.write(eLength);
        os.write(e);
        os.write(mLength);
        os.write(m);

        return os.toByteArray();
    }

    public static String getPublicKey(String privateSshKey, String privateSshKeyPassphrase) throws IOException, NotImplementedException {

        PEMKeyPair keyPair;
        RSAPublicKey rsaPubKey;
        Object keyObj = new PEMParser(new StringReader(privateSshKey)).readObject();

        // Key may be encrypted
        if (keyObj instanceof PEMKeyPair) {
            keyPair = (PEMKeyPair) keyObj;
        } else {
            // We need id_hmacWithSHA3_224 for encrypted ssh keys
            Security.addProvider(new BouncyCastleProvider());
            PEMEncryptedKeyPair encKeyPair = (PEMEncryptedKeyPair) keyObj;
            PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().setProvider("BC").build(privateSshKeyPassphrase.toCharArray());
            keyPair = encKeyPair.decryptKeyPair(decryptionProv);
        }

        try {
            rsaPubKey = (RSAPublicKey) new JcaPEMKeyConverter().getPublicKey(keyPair.getPublicKeyInfo());
        } catch (ClassCastException e) {
            throw new NotImplementedException("Only RSA SSH keys are currently supported");
        }

        byte[] pubKeyBody = getSshPublicKeyBody(rsaPubKey);
        String b64PubkeyBody = new String(java.util.Base64.getEncoder().encode(pubKeyBody), "UTF-8");

        return "ssh-rsa " + b64PubkeyBody;
    }
}
