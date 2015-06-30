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
package org.sonatype.nexus.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Handler;

import javax.annotation.Nullable;

import org.sonatype.nexus.bootstrap.jetty.JettyServer;
import org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread;
import org.sonatype.nexus.bootstrap.monitor.KeepAliveThread;
import org.sonatype.nexus.bootstrap.monitor.commands.ExitCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.HaltCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.PingCommand;
import org.sonatype.nexus.bootstrap.monitor.commands.StopApplicationCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.sonatype.nexus.bootstrap.monitor.CommandMonitorThread.LOCALHOST;
import static org.sonatype.nexus.bootstrap.monitor.KeepAliveThread.KEEP_ALIVE_PING_INTERVAL;
import static org.sonatype.nexus.bootstrap.monitor.KeepAliveThread.KEEP_ALIVE_PORT;
import static org.sonatype.nexus.bootstrap.monitor.KeepAliveThread.KEEP_ALIVE_TIMEOUT;

/**
 * Nexus bootstrap launcher.
 *
 * @since 2.1
 */
public class Launcher
{
  static {
    boolean hasJulBridge;
    try {
      // check whether we have access to the optional JUL->SLF4J logging bridge
      hasJulBridge = Handler.class.isAssignableFrom(org.slf4j.bridge.SLF4JBridgeHandler.class);
    } catch (Exception|LinkageError e) {
      hasJulBridge = false;
    }
    HAS_JUL_BRIDGE = hasJulBridge;
  }

  private static final boolean HAS_JUL_BRIDGE;

  public static final String IGNORE_SHUTDOWN_HELPER = ShutdownHelper.class.getName() + ".ignore";

  // FIXME: Move this to CommandMonitorThread
  public static final String COMMAND_MONITOR_PORT = CommandMonitorThread.class.getName() + ".port";

  public static final String SYSTEM_USERID = "*SYSTEM";

  private static final String FIVE_SECONDS = "5000";

  private static final String ONE_SECOND = "1000";

  private final JettyServer server;

  public Launcher(final @Nullable ClassLoader classLoader,
                  final @Nullable Map<String, String> overrides,
                  final String[] args)
      throws Exception
  {
    this(null, classLoader, overrides, args);
  }
  
  public Launcher(final @Nullable String basePath,
                  final @Nullable ClassLoader classLoader,
                  final @Nullable Map<String, String> overrides,
                  final String[] args)
      throws Exception
  {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException("Missing args");
    }

    if (HAS_JUL_BRIDGE) {
      org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
      org.slf4j.bridge.SLF4JBridgeHandler.install();
    }

    File baseDir = new File(basePath == null ? "." : basePath).getCanonicalFile();
    ClassLoader cl = (classLoader == null) ? getClass().getClassLoader() : classLoader;

    ConfigurationBuilder builder = new ConfigurationBuilder().defaults();

    builder.set("nexus-base", baseDir.getPath());
    if (basePath != null) {
      // look for configuration relative to the base
      builder.properties(new File(baseDir, "etc/nexus.properties"), true);
      builder.properties(new File(baseDir, "etc/nexus-test.properties"), false);
    }
    else {
      // search the classpath for the configuration
      builder.properties("/nexus.properties", true);
      builder.properties("/nexus-test.properties", false);
    }

    builder.custom(new EnvironmentVariables());
    builder.override(System.getProperties());

    if (overrides != null) {
      // using properties() instead of override() so we get all values added, not just those with existing entries
      builder.properties(overrides);
    }

    Map<String, String> props = builder.build();
    System.getProperties().putAll(props);
    ConfigurationHolder.set(props);

    // log critical information about the runtime environment
    Logger log = LoggerFactory.getLogger(Launcher.class);
    log.info("Java: {}, {}, {}, {}",
        System.getProperty("java.version"),
        System.getProperty("java.vm.name"),
        System.getProperty("java.vm.vendor"),
        System.getProperty("java.vm.version")
    );
    log.info("OS: {}, {}, {}",
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        System.getProperty("os.arch")
    );
    log.info("User: {}, {}, {}",
        System.getProperty("user.name"),
        System.getProperty("user.language"),
        System.getProperty("user.home")
    );
    log.info("CWD: {}", System.getProperty("user.dir"));

    // ensure the temporary directory is sane
    File tmpdir = TemporaryDirectory.get();
    log.info("TMP: {}", tmpdir);

    if (!"false".equalsIgnoreCase(getProperty(IGNORE_SHUTDOWN_HELPER, "false"))) {
      log.warn("ShutdownHelper requests will be ignored!");
      ShutdownHelper.setDelegate(ShutdownHelper.NOOP);
    }

    this.server = new JettyServer(cl, props, args);
  }

  public void start() throws Exception {
    start(true);
  }

  public void startAsync() throws Exception {
    start(false);
  }

  private void start(boolean waitForServer) throws Exception {
    maybeEnableCommandMonitor();
    maybeEnableShutdownIfNotAlive();

    server.start(waitForServer);
  }

  private String getProperty(final String name, final String defaultValue) {
    String value = System.getProperty(name, System.getenv(name));
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  private void maybeEnableCommandMonitor() throws IOException {
    String port = getProperty(COMMAND_MONITOR_PORT, null);
    if (port != null) {
      new CommandMonitorThread(
          Integer.parseInt(port),
          new StopApplicationCommand(new Runnable()
          {
            @Override
            public void run() {
              Launcher.this.commandStop();
            }
          }),
          new PingCommand(),
          new ExitCommand(),
          new HaltCommand()
      ).start();
    }
  }

  private void maybeEnableShutdownIfNotAlive() throws IOException {
    String port = getProperty(KEEP_ALIVE_PORT, null);
    if (port != null) {
      String pingInterval = getProperty(KEEP_ALIVE_PING_INTERVAL, FIVE_SECONDS);
      String timeout = getProperty(KEEP_ALIVE_TIMEOUT, ONE_SECOND);

      new KeepAliveThread(
          LOCALHOST,
          Integer.parseInt(port),
          Integer.parseInt(pingInterval),
          Integer.parseInt(timeout)
      ).start();
    }
  }

  public void commandStop() {
    ShutdownHelper.exit(0);
  }

  public void stop() throws Exception {
    server.stop();
  }

  public static void main(final String[] args) throws Exception {
    MDC.put("userId", SYSTEM_USERID);
    new Launcher(null, null, args).start();
  }
}
