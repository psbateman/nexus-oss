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
package org.sonatype.nexus.repository.view;

import java.util.Date;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.time.DateHelper.toDate;
import static org.sonatype.nexus.common.time.DateHelper.toDateTime;

/**
 * Content informations, set of auxiliary informations usually provided by some external means, like upstream proxy or
 * client.
 *
 * @since 3.0
 */
public class ContentInfo
{
  private static final String P_CONTENT = "content";

  private static final String P_LAST_MODIFIED = "last_modified";

  private static final String P_ETAG = "etag";

  @Nullable
  private final DateTime lastModified;

  @Nullable
  private final String etag;

  public ContentInfo(@Nullable final DateTime lastModified,
                     @Nullable final String etag)
  {
    this.lastModified = lastModified;
    this.etag = etag;
  }

  @Nullable
  public DateTime getLastModified() {
    return lastModified;
  }

  @Nullable
  public String getEtag() {
    return etag;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "lastModified=" + lastModified +
        ", etag='" + etag + '\'' +
        '}';
  }

  public static boolean hasLastModified(final Asset asset) {
    checkNotNull(asset);
    final NestedAttributesMap content = asset.attributes().child(P_CONTENT);
    return content.contains(P_LAST_MODIFIED);
  }

  public static ContentInfo extract(final Asset asset) {
    checkNotNull(asset);
    final NestedAttributesMap content = asset.attributes().child(P_CONTENT);
    final DateTime lastModified = toDateTime(content.get(P_LAST_MODIFIED, Date.class));
    final String etag = content.get(P_ETAG, String.class);
    return new ContentInfo(lastModified, etag);
  }

  public static void apply(final Asset asset, final ContentInfo contentInfo) {
    checkNotNull(asset);
    checkNotNull(contentInfo);
    final NestedAttributesMap content = asset.attributes().child(P_CONTENT);
    content.set(P_LAST_MODIFIED, toDate(contentInfo.getLastModified()));
    content.set(P_ETAG, contentInfo.getEtag());
  }
}
