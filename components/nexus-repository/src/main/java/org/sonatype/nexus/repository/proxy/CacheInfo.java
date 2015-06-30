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
package org.sonatype.nexus.repository.proxy;

import java.util.Date;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.time.DateHelper.toDate;
import static org.sonatype.nexus.common.time.DateHelper.toDateTime;

/**
 * A proxy cache info for given element.
 *
 * @since 3.0
 */
public class CacheInfo
{
  private static final String P_PROXY_CACHE = "proxy_cache";

  private static final String P_LAST_VERIFIED = "last_verified";

  private static final String P_CACHE_TOKEN = "cache_token";

  private final DateTime lastVerified;

  @Nullable
  private final String cacheToken;

  public CacheInfo(final DateTime lastVerified, @Nullable final String cacheToken) {
    this.lastVerified = checkNotNull(lastVerified);
    this.cacheToken = cacheToken;
  }

  public DateTime getLastVerified() {
    return lastVerified;
  }

  @Nullable
  public String getCacheToken() {
    return cacheToken;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "lastVerified=" + lastVerified +
        ", cacheToken='" + cacheToken + '\'' +
        '}';
  }

  @Nullable
  public static CacheInfo extract(final Asset asset) {
    checkNotNull(asset);
    final NestedAttributesMap proxyCache = asset.attributes().child(P_PROXY_CACHE);
    final DateTime lastVerified = toDateTime(proxyCache.get(P_LAST_VERIFIED, Date.class));
    if (lastVerified == null) {
      return null;
    }
    final String cacheToken = proxyCache.get(P_CACHE_TOKEN, String.class);
    return new CacheInfo(lastVerified, cacheToken);
  }

  public static void apply(final Asset asset, final CacheInfo cacheInfo) {
    checkNotNull(asset);
    checkNotNull(cacheInfo);
    final NestedAttributesMap proxyCache = asset.attributes().child(P_PROXY_CACHE);
    proxyCache.set(P_LAST_VERIFIED, toDate(cacheInfo.getLastVerified()));
    proxyCache.set(P_CACHE_TOKEN, cacheInfo.getCacheToken());
  }
}
