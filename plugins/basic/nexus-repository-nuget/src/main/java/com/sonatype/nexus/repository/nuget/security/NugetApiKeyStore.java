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
package com.sonatype.nexus.repository.nuget.security;

import javax.annotation.Nullable;

import org.sonatype.sisu.goodies.lifecycle.Lifecycle;

import org.apache.shiro.subject.PrincipalCollection;

/**
 * Persistent mapping between principals (such as user IDs) and API-Keys.
 *
 * @since 3.0
 */
public interface NugetApiKeyStore
    extends Lifecycle
{
  /**
   * Creates an API-Key and assigns it to the given principals.
   */
  char[] createApiKey(PrincipalCollection principals);

  /**
   * Gets the current API-Key assigned to the given principals.
   *
   * @return {@code null} if no key has been assigned
   */
  @Nullable
  char[] getApiKey(PrincipalCollection principals);

  /**
   * Retrieves the principals associated with the given API-Key.
   *
   * @return {@code null} if the key is invalid or stale
   */
  @Nullable
  PrincipalCollection getPrincipals(char[] apiKey);

  /**
   * Deletes the API-Key associated with the given principals.
   */
  void deleteApiKey(PrincipalCollection principals);

  /**
   * Purges any API-Keys associated with missing/deleted users.
   */
  void purgeApiKeys();
}
