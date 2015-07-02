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
package org.sonatype.nexus.internal.httpclient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.httpclient.SSLContextSelector;
import org.sonatype.sisu.goodies.common.Time;
import org.sonatype.sisu.goodies.lifecycle.Lifecycle;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.conn.DefaultHttpClientConnectionOperator;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTP;
import static org.sonatype.nexus.httpclient.HttpSchemes.HTTPS;

// TODO: Restore JMX support for httpclient bits

/**
 * Shared {@link PoolingHttpClientConnectionManager}.
 *
 * @since 3.0
 */
@Named("shared")
@Singleton
@SuppressWarnings("PackageAccessibility") // FIXME: httpclient usage is producing lots of OSGI warnings in IDEA
public class SharedHttpClientConnectionManager
    extends PoolingHttpClientConnectionManager
    implements Lifecycle
{
  private static final Logger log = LoggerFactory.getLogger(SharedHttpClientConnectionManager.class);

  private final Time connectionPoolIdleTime;

  private final Time connectionPoolEvictingDelayTime;

  private ConnectionEvictionThread evictionThread;

  @Inject
  public SharedHttpClientConnectionManager(
      final List<SSLContextSelector> sslContextSelectors,
      @Named("${nexus.httpclient.connectionpool.size:-20}") final int connectionPoolSize,
      @Named("${nexus.httpclient.connectionpool.maxSize:-200}") final int connectionPoolMaxSize,
      @Named("${nexus.httpclient.connectionpool.idleTime:-30s}") final Time connectionPoolIdleTime,
      @Named("${nexus.httpclient.connectionpool.evictingDelayTime:-5s}") final Time connectionPoolEvictingDelayTime,
      @Named("${nexus.httpclient.connectionpool.validateAfterInactivityTime:-2s}") final Time connectionPoolValidateAfterInactivityTime)
  {
    super(
        new DefaultHttpClientConnectionOperator(createRegistry(sslContextSelectors), null, null),
        null,
        connectionPoolIdleTime.toMillis(),
        TimeUnit.MILLISECONDS
    );

    setMaxTotal(connectionPoolMaxSize);
    log.debug("Connection pool max-size: {}", connectionPoolMaxSize);

    setDefaultMaxPerRoute(Math.min(connectionPoolSize, connectionPoolMaxSize));
    log.debug("Connection pool size: {}", connectionPoolSize);

    this.connectionPoolIdleTime = checkNotNull(connectionPoolIdleTime);
    this.connectionPoolEvictingDelayTime = checkNotNull(connectionPoolEvictingDelayTime);
    setValidateAfterInactivity(connectionPoolValidateAfterInactivityTime.toMillisI());
    log.debug("Connection pool idle-time: {}, evicting delay: {}, validate after inactivity: {}",
        connectionPoolIdleTime, connectionPoolEvictingDelayTime, connectionPoolValidateAfterInactivityTime);
  }

  private static Registry<ConnectionSocketFactory> createRegistry(final List<SSLContextSelector> sslContextSelectors) {
    RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
    builder.register(HTTP, PlainConnectionSocketFactory.getSocketFactory());
    builder.register(HTTPS, new NexusSSLConnectionSocketFactory(sslContextSelectors));
    return builder.build();
  }

  /**
   * Do nothing in order to avoid unwanted shutdown of shared connection manager.
   *
   * @see #stop()
   */
  @Override
  public void shutdown() {
    // empty
  }

  //
  // Lifecycle
  //

  // TODO: Maybe better to delegate to use lifecycle framework?

  @Override
  public Lifecycle getLifecycle() {
    return this;
  }

  @Override
  public void start() throws Exception {
    evictionThread = new ConnectionEvictionThread(this, connectionPoolIdleTime, connectionPoolEvictingDelayTime);
    evictionThread.start();
  }

  @Override
  public void stop() throws Exception {
    evictionThread.interrupt();
    evictionThread = null;

    super.shutdown();
  }
}
