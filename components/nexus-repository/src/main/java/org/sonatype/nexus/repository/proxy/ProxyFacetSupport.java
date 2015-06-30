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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.net.URI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.InvalidContentException;
import org.sonatype.nexus.repository.MissingFacetException;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.config.ConfigurationFacet;
import org.sonatype.nexus.repository.httpclient.HttpClientFacet;
import org.sonatype.nexus.repository.negativecache.NegativeCacheFacet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.ContentInfo;
import org.sonatype.nexus.repository.view.Context;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.HttpEntityPayload;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A support class which implements basic payload logic; subclasses provide format-specific operations.
 *
 * @since 3.0
 */
public abstract class ProxyFacetSupport
    extends FacetSupport
    implements ProxyFacet
{
  @VisibleForTesting
  static final String CONFIG_KEY = "proxy";

  @VisibleForTesting
  static class Config
  {
    @NotNull
    public URI remoteUrl;

    /**
     * Artifact max-age minutes.
     */
    @NotNull
    public int artifactMaxAge;

    @Override
    public String toString() {
      return getClass().getSimpleName() + "{" +
          "remoteUrl=" + remoteUrl +
          ", artifactMaxAge=" + artifactMaxAge +
          '}';
    }
  }

  private Config config;

  private HttpClientFacet httpClient;

  private boolean remoteUrlChanged;

  private String cacheToken;

  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    facet(ConfigurationFacet.class).validateSection(configuration, CONFIG_KEY, Config.class);
  }

  @Override
  protected void doConfigure(final Configuration configuration) throws Exception {
    config = facet(ConfigurationFacet.class).readSection(configuration, CONFIG_KEY, Config.class);

    // normalize URL path to contain trailing slash
    if (!config.remoteUrl.getPath().endsWith("/")) {
      config.remoteUrl = config.remoteUrl.resolve(config.remoteUrl.getPath() + "/");
    }

    log.debug("Config: {}", config);
  }

  @Override
  protected void doUpdate(final Configuration configuration) throws Exception {
    // detect URL changes
    URI previousUrl = config.remoteUrl;
    super.doUpdate(configuration);
    remoteUrlChanged = !config.remoteUrl.equals(previousUrl);
  }

  @Override
  protected void doDestroy() throws Exception {
    config = null;
  }

  @Override
  protected void doStart() throws Exception {
    httpClient = facet(HttpClientFacet.class);

    if (remoteUrlChanged) {
      remoteUrlChanged = false;

      try {
        facet(NegativeCacheFacet.class).invalidate();
      }
      catch (MissingFacetException e) {
        // NCF is optional
      }
    }
  }

  @Override
  protected void doStop() throws Exception {
    httpClient = null;
  }

  public URI getRemoteUrl() {
    return config.remoteUrl;
  }

  @Override
  public Content get(final Context context) throws IOException {
    checkNotNull(context);

    Content content = getCachedPayload(context);

    if (isStale(context, content)) {
      try {
        final Content remote = fetch(context, content);
        if (remote != null) {
          store(context, remote);
          content = getCachedPayload(context);
        }
      }
      catch (IOException e) {
        log.warn("Failed to fetch: {}", getUrl(context), e);
        throw e;
      }
    }
    return content;
  }

  @Override
  public void invalidateProxyCaches() {
    log.info("Expiring proxy caches of {}", getRepository().getName());
    this.cacheToken = Long.toString(System.nanoTime());
  }

  /**
   * If we have the content cached locally already, return that - otherwise {@code null}.
   */
  protected abstract Content getCachedPayload(final Context context) throws IOException;

  /**
   * Store a new Payload, freshly fetched from the remote URL. The Context indicates which component
   * was being requested.
   */
  protected abstract void store(final Context context, final Content content)
      throws IOException, InvalidContentException;

  @Nullable
  protected Content fetch(final Context context, Content stale) throws IOException {
    return fetch(getUrl(context), context, stale);
  }

  protected Content fetch(String url, Context context, Content stale) throws IOException {
    HttpClient client = httpClient.getHttpClient();

    HttpGet request = new HttpGet(config.remoteUrl.resolve(url));
    if (stale != null) {
      final ContentInfo staleInfo = stale.getAttributes().get(ContentInfo.class);
      if (staleInfo != null && staleInfo.getLastModified() != null) {
        request.addHeader(HttpHeaders.IF_MODIFIED_SINCE, DateUtils.formatDate(staleInfo.getLastModified().toDate()));
      }
      if (staleInfo != null && staleInfo.getEtag() != null) {
        request.addHeader(HttpHeaders.IF_NONE_MATCH, "\"" + staleInfo.getEtag() + "\"");
      }
    }
    log.debug("Fetching: {}", request);

    HttpResponse response = client.execute(request);
    log.debug("Response: {}", response);

    StatusLine status = response.getStatusLine();
    log.debug("Status: {}", status);

    final CacheInfo cacheInfo = new CacheInfo(DateTime.now(), cacheToken);

    if (status.getStatusCode() == HttpStatus.SC_OK) {
      HttpEntity entity = response.getEntity();
      log.debug("Entity: {}", entity);

      Payload payload = new HttpEntityPayload(response, entity);
      final Content result = new Content(payload);
      final ContentInfo contentInfo = new ContentInfo(
          extractLastModified(request, response.getLastHeader(HttpHeaders.LAST_MODIFIED)),
          extractETag(response.getLastHeader(HttpHeaders.ETAG))
      );
      result.getAttributes().set(ContentInfo.class, contentInfo);
      result.getAttributes().set(CacheInfo.class, cacheInfo);
      return result;
    }
    if (status.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
      indicateVerified(context, cacheInfo);
    }
    HttpClientUtils.closeQuietly(response);

    return null;
  }

  /**
   * Extract Last-Modified date from response if possible, or {@code null}.
   */
  @Nullable
  private DateTime extractLastModified(final HttpGet request, final Header lastModifiedHeader) {
    if (lastModifiedHeader != null) {
      try {
        return new DateTime(DateUtils.parseDate(lastModifiedHeader.getValue()).getTime());
      }
      catch (Exception ex) {
        log.warn("Could not parse date '{}' received from {}; using system current time as item creation time",
            lastModifiedHeader, request.getURI());
      }
    }
    return null;
  }

  /**
   * Extract ETag from response if possible, or {@code null}.
   */
  @Nullable
  private String extractETag(final Header etagHeader) {
    if (etagHeader != null) {
      final String etag = etagHeader.getValue();
      if (!Strings.isNullOrEmpty(etag)) {
        if (etag.startsWith("\"") && etag.endsWith("\"")) {
          return etag.substring(1, etag.length() - 1);
        }
        else {
          return etag;
        }
      }
    }
    return null;
  }

  /**
   * For whatever component/asset
   */
  protected abstract void indicateVerified(final Context context, final CacheInfo cacheInfo) throws IOException;

  /**
   * Provide the URL of the content relative to the repository root.
   */
  protected abstract String getUrl(final @Nonnull Context context);

  private boolean isStale(final Context context, final Content content) throws IOException {
    if (content == null) {
      // not in cache
      return true;
    }
    if (config.artifactMaxAge < 0) {
      log.trace("Content max age checking disabled");
      return false;
    }

    final CacheInfo cacheInfo = content.getAttributes().get(CacheInfo.class);;
    if (cacheInfo == null) {
      log.debug("Content cache info unknown");
      return true;
    }
    if (cacheToken != null && !cacheToken.equals(cacheInfo.getCacheToken())) {
      log.debug("Content expired (cacheToken)");
      return true;
    }
    if (cacheInfo.getLastVerified().isBefore(new DateTime().minusMinutes(config.artifactMaxAge))) {
      log.debug("Content expired (age)");
      return true;
    }
    return false;
  }
}
