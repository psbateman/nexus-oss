/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.maven.internal;

/**
 * Maven format specific attributes.
 *
 * @since 3.0
 */
public interface MavenAttributes
{
  // artifact shared properties of both, artifact component and artifact asset

  String P_GROUP_ID = "groupId";

  String P_ARTIFACT_ID = "artifactId";

  String P_VERSION = "version";

  String P_BASE_VERSION = "baseVersion";

  String P_CLASSIFIER = "classifier";

  String P_EXTENSION = "extension";

  // artifact component properties

  String P_COMPONENT_KEY = "key";

  // shared properties for both artifact and metadata assets

  String P_ASSET_KEY = "key";

  String P_CONTENT_LAST_MODIFIED = "contentLastModified";

  String P_CONTENT_ETAG = "contentEtag";

  String P_LAST_VERIFIED = "lastVerified";
}
