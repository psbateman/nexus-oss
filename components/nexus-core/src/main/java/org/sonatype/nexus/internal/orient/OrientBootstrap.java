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
package org.sonatype.nexus.internal.orient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.DatabaseManager;
import org.sonatype.nexus.orient.DatabaseServer;
import org.sonatype.sisu.goodies.lifecycle.LifecycleSupport;
import org.sonatype.sisu.goodies.lifecycle.Lifecycles;

import com.orientechnologies.orient.core.compression.OCompression;
import com.orientechnologies.orient.core.compression.OCompressionFactory;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Orient bootstrap.
 *
 * @since 3.0
 */
@Named
@Singleton
public class OrientBootstrap
    extends LifecycleSupport
{
  private final Provider<DatabaseServer> databaseServer;

  private final Provider<DatabaseManager> databaseManager;

  @Inject
  public OrientBootstrap(final Provider<DatabaseServer> databaseServer,
                         final Provider<DatabaseManager> databaseManager,
                         final Iterable<OCompression> managedCompressions,
                         final Iterable<OSQLFunctionAbstract> functions)
  {
    this.databaseServer = checkNotNull(databaseServer);
    this.databaseManager = checkNotNull(databaseManager);
    registerCompressions(checkNotNull(managedCompressions));
    registerCustomFunctions(checkNotNull(functions));
  }


  @Override
  protected void doStart() throws Exception {
    databaseServer.get().start();

    Lifecycles.start(databaseManager.get());
  }

  @Override
  protected void doStop() throws Exception {
    Lifecycles.stop(databaseManager.get());

    databaseServer.get().stop();
  }

  private void registerCompressions(final Iterable<OCompression> compressions) {
    for (final OCompression compression : compressions) {
      try {
        log.debug("Registering OrientDB compression {} as '{}'", compression, compression.name());
        OCompressionFactory.INSTANCE.register(compression);
      }
      catch (final IllegalArgumentException e) {
        log.debug("An OrientDB compression named '{}' was already registered", compression.name(), e);
      }
    }
  }

  private void registerCustomFunctions(final Iterable<OSQLFunctionAbstract> functions) {
    log.debug("Registering custom OrientDB functions");
    for (OSQLFunctionAbstract function : functions) {
      log.debug("Registering OrientDB function " + function.getName());
      OSQLEngine.getInstance().registerFunction(function.getName(), function);
    }
  }
}