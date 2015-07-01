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
package org.sonatype.nexus.testsuite.maven.chs;

import javax.inject.Inject;

import org.sonatype.nexus.log.LogManager;
import org.sonatype.nexus.log.LoggerLevel;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.maven.policy.VersionPolicy;
import org.sonatype.nexus.testsuite.maven.Maven2Client;
import org.sonatype.nexus.testsuite.maven.MavenITSupport;
import org.sonatype.tests.http.server.fluent.Server;
import org.sonatype.tests.http.server.jetty.behaviour.PathRecorderBehaviour;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Option;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

/**
 * Maven Concurrency Hotspot IT support.
 */
public abstract class MavenHotspotITSupport
    extends MavenITSupport
{
  @org.ops4j.pax.exam.Configuration
  public static Option[] configureNexus() {
    return options(nexusDistribution("org.sonatype.nexus.assemblies", "nexus-base-template"),
        // set start-level to just after the nexus edition has been installed so we can re-use its dependencies
        mavenBundle("org.sonatype.http-testing-harness", "server-provider").versionAsInProject().startLevel(101)
    );
  }

  protected static final String RELEASE_XML_ARTIFACT_PATH = "group/artifact/1.0/artifact-1.0.xml";

  protected static final String RELEASE_ZIP_ARTIFACT_PATH = "group/artifact/1.0/artifact-1.0.zip";

  protected static final String RELEASE_TXT_ARTIFACT_PATH = "group/artifact/1.0/artifact-1.0.txt";

  @Rule
  public TestName testName = new TestName();

  @Inject
  protected RepositoryManager repositoryManager;

  @Inject
  private LogManager logManager;

  protected PathRecorderBehaviour pathRecorderBehaviour;

  protected GeneratorBehaviour xmlArtifactGenerator;

  protected GeneratorBehaviour zipArtifactGenerator;

  protected GeneratorBehaviour txtArtifactGenerator;

  private Repository repository;

  private Server upstream;

  protected Maven2Client repositoryClient;

  @Before
  public void setupMavenDebugStorage() {
    logManager.setLoggerLevel("org.sonatype.nexus.repository.storage", LoggerLevel.DEBUG);
  }

  @Before
  public void prepare() throws Exception {
    pathRecorderBehaviour = new PathRecorderBehaviour();
    xmlArtifactGenerator = new GeneratorBehaviour(new XmlGenerator());
    zipArtifactGenerator = new GeneratorBehaviour(new ZipGenerator());
    txtArtifactGenerator = new GeneratorBehaviour(new TextGenerator());

    upstream = Server.withPort(0)
        .serve("/" + RELEASE_XML_ARTIFACT_PATH).withBehaviours(
            pathRecorderBehaviour,
            xmlArtifactGenerator
        )
        .serve("/" + RELEASE_ZIP_ARTIFACT_PATH).withBehaviours(
            pathRecorderBehaviour,
            zipArtifactGenerator
        )
        .serve("/" + RELEASE_TXT_ARTIFACT_PATH).withBehaviours(
            pathRecorderBehaviour,
            txtArtifactGenerator
        )
        .start();

    final Configuration configuration = proxyConfig(testName.getMethodName(),
        "http://localhost:" + upstream.getPort() + "/", VersionPolicy.RELEASE);
    repository = repositoryManager.create(configuration);
    assertThat(repository, notNullValue());

    repositoryClient = new Maven2Client(HttpClients.custom().build(), HttpClientContext.create(),
        resolveUrl(nexusUrl, "/repository/" + repository.getName() + "/").toURI());
  }

  @After
  public void cleanup()
      throws Exception
  {
    if (upstream != null) {
      upstream.stop();
    }
  }
}
