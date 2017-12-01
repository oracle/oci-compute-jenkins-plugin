/**
 * Jenkins OCI Plugin
 *
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
package org.jenkinsci.plugins.oci.utils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import com.google.common.base.Supplier;

/**
 * Provide private key as a String for
 * {@link com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider}.
 *
 * @author Filip Krestan
 */
public class StringPrivateKeySupplier implements Supplier<InputStream> {

  private final String key;

  public StringPrivateKeySupplier(String key) {
    this.key = key;
  }

  @Override
  public InputStream get() {
    try {
      return new ByteArrayInputStream(key.getBytes(StandardCharsets.UTF_8.name()));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Bad key encoding. Expected: utf-8", e);
    }
  }

  @Override
  public String toString() {
    return key;
  }
}
