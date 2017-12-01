/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

public class KeyUtils {

  /**
   * Convert data to colon-delimited hexadecimal string.
   *
   * @param data
   *            data to be converted
   * @return colon-delimited hexadecimal string
   */
  public static String toHex(byte[] data) {
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < data.length; i++) {
      sb.append(Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1));
      if (i + 1 < data.length) {
        sb.append(':');
      }
    }
    return sb.toString();
  }

  /**
   * MD5 hash in byte format. Use {{@link #toHex(byte[])}} to get ssh-keygen
   * compatible fingerprint.
   *
   * @param data
   *            Data to be hashed
   * @return MD5 hash in byte format
   * @throws NoSuchAlgorithmException
   *             when MD5 not available on the system
   * @throws IOException
   *             On reader error
   */
  public static byte[] MD5(byte[] data) throws NoSuchAlgorithmException, IOException {

    MessageDigest md = MessageDigest.getInstance("MD5");
    InputStream is = new ByteArrayInputStream(data);

    byte[] dataBytes = new byte[1024];
    int nread = 0;

    while ((nread = is.read(dataBytes)) != -1) {
      md.update(dataBytes, 0, nread);
    }

    return md.digest();
  }

  /**
   * Convert private ssh key in PEM format to ssh-specific public key defined
   * in <a href=https://tools.ietf.org/html/rfc4253#section-6.6>rfc4253</a>
   * and <a href=https://tools.ietf.org/html/rfc4716>rfc4716</a>.
   * <p>
   * Currently only RSA keys are supported.
   *
   * @param rsaPubKey
   *            RSA public key
   * @return body of the ssh-specific public key
   */
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

  /**
   * Convert private ssh key in PEM format to ssh-specific public key
   *
   * Currently only RSA keys are supported.
   *
   * @param privateSshKey
   *            Private ssh key in PEM format
   * @param privateSshKeyPassphrase
   *            Passphrase for ssh key if encrypted, <code>null</code>
   *            otherwise
   * @return public key in ssh-specific format (same as in authorized_keys)
   * @throws NotImplementedException
   *             If not RSA key
   * @throws IOException
   *             On reader error
   */
  public static String getSshPublicKey(String privateSshKey, String privateSshKeyPassphrase) throws IOException, NotImplementedException {

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
    String b64PubkeyBody = new String(Base64.getEncoder().encode(pubKeyBody), "UTF-8");

    return "ssh-rsa " + b64PubkeyBody;
  }

  /**
   * Get key fingerprint in OCI-specific format from private SSH key in PEM
   * format
   *
   * The fingerprint is same as:
   * <code>openssl rsa -pubout -outform DER -in key.pem | openssl md5 -c</code>
   *
   * @param reader
   *            Private ssh key in PEM format
   * @return public ssh key fingerprint
   * @throws NoSuchAlgorithmException
   *             when MD5 not available on the system
   * @throws IOException
   *             On reader error
   */
  public static String getDERKeyFingerprint(Reader reader) throws IOException, NoSuchAlgorithmException {
    PEMKeyPair keyPair = (PEMKeyPair) new PEMParser(reader).readObject();
    return toHex(MD5(keyPair.getPublicKeyInfo().getEncoded(ASN1Encoding.DER)));
  }
}
