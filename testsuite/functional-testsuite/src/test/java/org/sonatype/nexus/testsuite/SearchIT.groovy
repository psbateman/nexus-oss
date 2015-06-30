/*
 * Copyright (c) 2008-2015 Sonatype, Inc.
 *
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/pro/attributions
 * Sonatype and Sonatype Nexus are trademarks of Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation.
 * M2Eclipse is a trademark of the Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.testsuite

import javax.inject.Inject

import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.manager.RepositoryManager
import org.sonatype.nexus.repository.maven.policy.VersionPolicy
import org.sonatype.nexus.repository.storage.WritePolicy

import org.junit.Before
import org.junit.Test

/**
 * Search related Siesta tests.
 */
class SearchIT
extends FunctionalTestSupport
{

  @Inject
  private RepositoryManager repositoryManager


  SearchIT(final String executable, final String[] options) {
    super(executable, options)
  }

  @Before
  void setupMaven() {
    def mavenRepo = 'search-test-maven'
    repositoryManager.create(new Configuration(
        repositoryName: mavenRepo,
        recipeName: 'maven2-hosted',
        online: true,
        attributes: [
            maven: [
                versionPolicy: VersionPolicy.RELEASE
            ],
            storage: [
                blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                writePolicy: WritePolicy.ALLOW_ONCE
            ]
        ]
    ))
    def nugetRepo = 'search-test-nuget'
    repositoryManager.create(new Configuration(
        repositoryName: nugetRepo,
        recipeName: 'nuget-hosted',
        online: true,
        attributes: [
            storage: [
                blobStoreName: BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                writePolicy: WritePolicy.ALLOW
            ]
        ]
    ))
  }

  @Test
  public void suite() {
    //run("search/.*\\.t.js")
  }
}
