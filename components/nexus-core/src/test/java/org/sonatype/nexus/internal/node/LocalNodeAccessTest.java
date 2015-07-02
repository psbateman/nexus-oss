/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.internal.node;

import java.io.File;
import java.security.cert.Certificate;

import org.sonatype.nexus.common.node.LocalNodeAccess;
import org.sonatype.sisu.goodies.crypto.internal.CryptoHelperImpl;
import org.sonatype.sisu.goodies.ssl.keystore.KeyStoreManager;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link LocalNodeAccess}.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class LocalNodeAccessTest
    extends TestSupport
{
  private KeyStoreManager keyStoreManager;

  private LocalNodeAccess localNodeAccess;

  @Before
  public void prepare() throws Exception {
    File dir = util.createTempDir("keystores");
    KeyStoreManagerConfigurationImpl config = new KeyStoreManagerConfigurationImpl(dir);
    // use lower strength for faster test execution
    config.setKeyAlgorithmSize(512);
    keyStoreManager = new KeyStoreManagerImpl(new CryptoHelperImpl(), config);
    keyStoreManager.generateAndStoreKeyPair("a", "b", "c", "d", "e", "f");

    localNodeAccess = new LocalNodeAccessImpl(keyStoreManager);
  }

  @Test
  public void idEqualToIdentityCertificate() throws Exception {
    Certificate cert = keyStoreManager.getCertificate();
    assertThat(localNodeAccess.getId(), equalTo(NodeIdEncoding.nodeIdForCertificate(cert)));
  }

  @Test
  public void idResets() throws Exception {
    Certificate cert1 = keyStoreManager.getCertificate();
    assertThat(localNodeAccess.getId(), equalTo(NodeIdEncoding.nodeIdForCertificate(cert1)));

    // Now replace the identity
    keyStoreManager.removePrivateKey();
    keyStoreManager.generateAndStoreKeyPair("a", "b", "c", "d", "e", "f");
    localNodeAccess.reset();

    // Ensure the certificate has changed
    Certificate cert2 = keyStoreManager.getCertificate();
    assertThat(cert2, not(cert1));

    // And the node id has changed too
    assertThat(localNodeAccess.getId(), equalTo(NodeIdEncoding.nodeIdForCertificate(cert2)));
  }
}
