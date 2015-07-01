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
package org.sonatype.nexus.timeline.feeds.sources;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.timeline.feeds.FeedEvent;
import org.sonatype.nexus.timeline.feeds.FeedRecorder;

@Named(RecentCachedArtifactFeedSource.CHANNEL_KEY)
@Singleton
public class RecentCachedArtifactFeedSource
    extends AbstractFeedSource
{
  public static final String CHANNEL_KEY = "recentlyCachedArtifacts";

  //private final RepositoryRegistry repositoryRegistry;

  @Inject
  public RecentCachedArtifactFeedSource(final FeedRecorder feedRecorder/*, final RepositoryRegistry repositoryRegistry*/) {
    super(feedRecorder,
        CHANNEL_KEY,
        "Cached artifacts",
        "New remote artifacts (cached).");
    //this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Override
  public void fillInEntries(final List<FeedEvent> entries, final int from, final int count,
                            final Map<String, String> params)
  {
    //entries.addAll(getFeedRecorder()
    //    .getEvents(ImmutableSet.of(FeedRecorder.FAMILY_ITEM), ImmutableSet.of(FeedRecorder.ITEM_CACHED, FeedRecorder.ITEM_CACHED_UPDATE), from, count,
    //        and(isMavenArtifact(repositoryRegistry), filters(params))
    //    ));
  }
}
