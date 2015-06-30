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
package org.sonatype.nexus.extender;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.log.LogManager;
import org.sonatype.sisu.goodies.lifecycle.Lifecycle;

import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.name.Names;
import com.google.inject.servlet.GuiceFilter;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.wire.ParameterKeys;
import org.eclipse.sisu.wire.WireModule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonMap;
import static org.apache.karaf.features.FeaturesService.Option.ContinueBatchOnFailure;
import static org.apache.karaf.features.FeaturesService.Option.NoAutoRefreshBundles;
import static org.apache.karaf.features.FeaturesService.Option.NoCleanIfFailure;

/**
 * {@link ServletContextListener} that bootstraps the core Nexus application.
 *
 * @since 3.0
 */
public class NexusContextListener
    implements ServletContextListener, FrameworkListener
{
  static {
    boolean hasPaxExam;
    try {
      // detect if running with Pax-Exam so we can register our locator
      hasPaxExam = org.ops4j.pax.exam.util.Injector.class.isInterface();
    }
    catch (final LinkageError e) {
      hasPaxExam = false;
    }
    HAS_PAX_EXAM = hasPaxExam;
  }

  private static final boolean HAS_PAX_EXAM;

  private static final int NEXUS_PLUGIN_START_LEVEL = 200;

  private static final Logger log = LoggerFactory.getLogger(NexusContextListener.class);

  private final NexusBundleExtender extender;

  private BundleContext bundleContext;

  private ServletContext servletContext;

  private FeaturesService featuresService;

  private Injector injector;

  private LogManager logManager;

  private Lifecycle application;

  private ServiceRegistration<Filter> registration;

  public NexusContextListener(final NexusBundleExtender extender) {
    this.extender = extender;
  }

  @Override
  public void contextInitialized(final ServletContextEvent event) {
    SharedMetricRegistries.getOrCreate("nexus");

    bundleContext = extender.getBundleContext();

    servletContext = event.getServletContext();
    Map<?, ?> nexusProperties = (Map<?, ?>) servletContext.getAttribute("nexus.properties");
    if (nexusProperties == null) {
      nexusProperties = System.getProperties();
    }

    featuresService = bundleContext.getService(bundleContext.getServiceReference(FeaturesService.class));

    injector = Guice.createInjector(new WireModule( //
        new NexusContextModule(bundleContext, servletContext, nexusProperties)));

    log.debug("Injector: {}", injector);

    extender.doStart(); // start tracking nexus bundles

    try {
      logManager = injector.getInstance(LogManager.class);
      log.debug("Log manager: {}", logManager);
      logManager.start();

      application = injector.getInstance(Key.get(Lifecycle.class, Names.named("NxApplication")));
      log.debug("Application: {}", application);

      final FrameworkStartLevel fsl = bundleContext.getBundle(0).adapt(FrameworkStartLevel.class);

      // assign higher start level to hold back plugin activation
      fsl.setInitialBundleStartLevel(NEXUS_PLUGIN_START_LEVEL);

      installFeatures(getFeatures((String) nexusProperties.get("nexus-features")));

      if (nexusProperties.containsKey("nexus-test-features")) {
        installFeatures(getFeatures((String) nexusProperties.get("nexus-test-features")));
      }

      // raise framework start level to activate plugins
      fsl.setStartLevel(NEXUS_PLUGIN_START_LEVEL, this);
    }
    catch (final Exception e) {
      log.error("Failed to lookup application", e);
      Throwables.propagate(e);
    }
  }

  public void frameworkEvent(final FrameworkEvent event) {
    if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
      // any local Nexus plugins have now been activated

      try {
        application.start();
      }
      catch (final Exception e) {
        log.error("Failed to start application", e);
        Throwables.propagate(e);
      }

      // register our dynamic filter with the surrounding bootstrap code
      final Filter filter = injector.getInstance(GuiceFilter.class);
      final Dictionary<String, ?> filterProperties = new Hashtable<>(singletonMap("name", "nexus"));
      registration = bundleContext.registerService(Filter.class, filter, filterProperties);

      if (HAS_PAX_EXAM) {
        registerLocatorWithPaxExam(injector.getProvider(BeanLocator.class));
      }
    }
  }

  @Override
  public void contextDestroyed(final ServletContextEvent event) {
    // remove our dynamic filter
    if (registration != null) {
      registration.unregister();
      registration = null;
    }

    if (application != null) {
      try {
        application.stop();
      }
      catch (final Exception e) {
        log.error("Failed to stop application", e);
      }
      application = null;
    }

    if (logManager != null) {
      try {
        logManager.stop();
      }
      catch (final Exception e) {
        log.error("Failed to stop log-manager", e);
      }
      logManager = null;
    }

    extender.doStop(); // stop tracking bundles

    if (servletContext != null) {
      servletContext = null;
    }

    injector = null;

    SharedMetricRegistries.remove("nexus");
  }

  public Injector getInjector() {
    checkState(injector != null, "Missing injector reference");
    return injector;
  }

  private Set<Feature> getFeatures(final String featureNames) throws Exception {
    final Set<Feature> features = new LinkedHashSet<>();
    if (featureNames != null) {
      log.info("Selecting features by name...");

      for (final String name : Splitter.on(',').trimResults().omitEmptyStrings().split(featureNames)) {
        final Feature feature = featuresService.getFeature(name);
        if (feature != null) {
          log.info("Adding {}", name);
          features.add(feature);
        }
        else {
          log.warn("Missing {}", name);
        }
      }
    }
    return features;
  }

  private void installFeatures(final Set<Feature> features) throws Exception {
    log.info("Installing selected features...");

    // install features using batch mode; skip features already in the cache
    features.removeAll(Arrays.asList(featuresService.listInstalledFeatures()));
    if (features.size() > 0) {
      final EnumSet<Option> options = EnumSet.of(ContinueBatchOnFailure, NoCleanIfFailure, NoAutoRefreshBundles);
      featuresService.installFeatures(features, options);
    }

    log.info("Installed {} features", features.size());
  }

  /**
   * Registers our locator service with Pax-Exam to handle injection of test classes.
   */
  private void registerLocatorWithPaxExam(final Provider<BeanLocator> locatorProvider) {

    // ensure this service is ranked higher than the Pax-Exam one
    final Dictionary<String, Object> examProperties = new Hashtable<>();
    examProperties.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
    examProperties.put("name", "nexus");

    bundleContext.registerService(org.ops4j.pax.exam.util.Injector.class, new org.ops4j.pax.exam.util.Injector()
    {
      public void injectFields(final Object target) {
        Guice.createInjector(new WireModule(new AbstractModule()
        {
          @Override
          protected void configure() {
            // support injection of application components by wiring via shared locator
            // (use provider to avoid auto-publishing test-instance to the application)
            bind(BeanLocator.class).toProvider(locatorProvider);

            // support injection of application properties
            bind(ParameterKeys.PROPERTIES).toInstance(
                locatorProvider.get().locate(ParameterKeys.PROPERTIES).iterator().next().getValue());

            // inject the test-instance
            requestInjection(target);
          }
        }));
      }
    }, examProperties);
  }
}
