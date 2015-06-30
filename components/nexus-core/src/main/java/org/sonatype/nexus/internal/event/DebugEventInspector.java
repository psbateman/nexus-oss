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
package org.sonatype.nexus.internal.event;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.jmx.reflect.ManagedAttribute;
import org.sonatype.nexus.jmx.reflect.ManagedObject;
import org.sonatype.sisu.goodies.common.ComponentSupport;
import org.sonatype.sisu.goodies.eventbus.EventBus;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.eclipse.sisu.EagerSingleton;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A simple "debug" helper component, that dumps out events to log. Usable for debugging or problem solving, not for
 * production use! It will register itself to listen for events only when enabled, otherwise it will not spend any CPU
 * cycles being dormant. It can be enabled via System property or JMX.
 *
 * @author cstamas
 * @since 2.1
 */
@Named
@EagerSingleton
@ManagedObject
public class DebugEventInspector
    extends ComponentSupport
{
  private final boolean ENABLED_DEFAULT = SystemPropertiesHelper.getBoolean(
      DebugEventInspector.class.getName() + ".enabled", false);

  private volatile boolean enabled;

  private final EventBus eventBus;

  @Inject
  public DebugEventInspector(final EventBus eventBus) {
    this.eventBus = checkNotNull(eventBus);
    setEnabled(ENABLED_DEFAULT);
  }

  @ManagedAttribute
  public boolean isEnabled() {
    return enabled;
  }

  @ManagedAttribute
  public void setEnabled(boolean enabled) {
    try {
      if (enabled && !this.enabled) {
        eventBus.register(this);
      }
      else if (!enabled && this.enabled) {
        eventBus.unregister(this);
      }
    }
    finally {
      this.enabled = enabled;
    }
  }

  @Subscribe
  @AllowConcurrentEvents
  public void accept(final Object event) {
    log.info("{}", event);
  }
}
