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
package org.sonatype.nexus.repository.raw.internal;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.io.TempStreamSupplier;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.proxy.CacheInfo;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.StorageFacet.Operation;
import org.sonatype.nexus.repository.storage.StorageTx;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentInfo;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_ATTRIBUTES;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_PATH;

/**
 * A {@link RawContentFacet} that persists to a {@link StorageFacet}.
 *
 * @since 3.0
 */
@Named
public class RawContentFacetImpl
    extends FacetSupport
    implements RawContentFacet
{
  private final static List<HashAlgorithm> hashAlgorithms = Lists.newArrayList(MD5, SHA1);

  // TODO: raw does not have config, this method is here only to have this bundle do Import-Package org.sonatype.nexus.repository.config
  // TODO: as FacetSupport subclass depends on it. Actually, this facet does not need any kind of configuration
  // TODO: it's here only to circumvent this OSGi/maven-bundle-plugin issue.
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    // empty
  }

  @Nullable
  @Override
  public Content get(final String path) {
    try (StorageTx tx = getStorage().openTx()) {
      final Component component = getComponent(tx, path, tx.getBucket());
      if (component == null) {
        return null;
      }

      final Asset asset = tx.firstAsset(component);
      final Blob blob = tx.requireBlob(asset.requireBlobRef());

      return marshall(asset, blob);
    }
  }

  @Override
  public void put(final String path, final Payload payload) throws IOException, InvalidContentException {
    try (final TempStreamSupplier streamSupplier = new TempStreamSupplier(payload.openInputStream())) {
      getStorage().perform(new Operation<Void>()
      {
        @Override
        public Void execute(final StorageTx tx) {
          try {
            final Bucket bucket = tx.getBucket();
            Component component = getComponent(tx, path, bucket);
            Asset asset;
            if (component == null) {
              // CREATE
              component = tx.createComponent(bucket, getRepository().getFormat())
                  .group(getGroup(path))
                  .name(getName(path));

              // Set attributes map to contain "raw" format-specific metadata (in this case, path)
              component.formatAttributes().set(P_PATH, path);
              tx.saveComponent(component);

              asset = tx.createAsset(bucket, component);
              asset.name(component.name());
            }
            else {
              // UPDATE
              asset = tx.firstAsset(component);
            }

            if (payload instanceof Content) {
              final Content content = (Content) payload;

              final ContentInfo contentInfo = content.getAttributes().get(ContentInfo.class);
              if (contentInfo != null) {
                ContentInfo.apply(asset, contentInfo);
              }
              else {
                ContentInfo.apply(asset, new ContentInfo(DateTime.now(), null));
              }

              final CacheInfo cacheInfo = content.getAttributes().get(CacheInfo.class);
              if (cacheInfo != null) {
                CacheInfo.apply(asset, cacheInfo);
              }
            }
            else {
              ContentInfo.apply(asset, new ContentInfo(DateTime.now(), null));
            }

            tx.setBlob(asset, path, streamSupplier.get(), hashAlgorithms, null, payload.getContentType());
            tx.saveAsset(asset);

            return null;
          }
          catch (IOException e) {
            throw Throwables.propagate(e);
          }
        }

        @Override
        public String toString() {
          return String.format("put(%s)", path);
        }
      });
    }
    catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      }
      throw e;
    }
  }

  private String getGroup(String path) {
    StringBuilder group = new StringBuilder();
    if (!path.startsWith("/")) {
      group.append("/");
    }
    int i = path.lastIndexOf("/");
    if (i != -1) {
      group.append(path.substring(0, i));
    }
    return group.toString();
  }

  private String getName(String path) {
    int i = path.lastIndexOf("/");
    if (i != -1) {
      return path.substring(i + 1);
    }
    else {
      return path;
    }
  }

  @Override
  public boolean delete(final String path) throws IOException {
    try (StorageTx tx = getStorage().openTx()) {
      final Component component = getComponent(tx, path, tx.getBucket());
      if (component == null) {
        return false;
      }

      tx.deleteComponent(component);
      tx.commit();
      return true;
    }
  }

  @Override
  public void setCacheInfo(final String path, final CacheInfo cacheInfo) throws IOException {
    getStorage().perform(new Operation<Void>()
    {
      @Override
      public Void execute(final StorageTx tx) {
        Component component = tx.findComponentWithProperty(P_PATH, path, tx.getBucket());

        if (component == null) {
          log.debug("Attempting to set last verified date for non-existent raw component {}", path);
          return null;
        }

        final Asset asset = tx.firstAsset(component);
        CacheInfo.apply(asset, cacheInfo);
        tx.saveAsset(tx.firstAsset(component));
        return null;
      }

      @Override
      public String toString() {
        return String.format("setCacheInfo(%s, %s)", path, cacheInfo);
      }
    });
  }

  private StorageFacet getStorage() {
    return getRepository().facet(StorageFacet.class);
  }

  // TODO: Consider a top-level indexed property (e.g. "locator") to make these common lookups fast
  private Component getComponent(StorageTx tx, String path, Bucket bucket) {
    String property = String.format("%s.%s.%s", P_ATTRIBUTES, RawFormat.NAME, P_PATH);
    return tx.findComponentWithProperty(property, path, bucket);
  }

  private Content marshall(final Asset asset, final Blob blob) {
    final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    content.getAttributes().set(ContentInfo.class, ContentInfo.extract(asset));
    content.getAttributes().set(CacheInfo.class, CacheInfo.extract(asset));
    return content;
  }
}
