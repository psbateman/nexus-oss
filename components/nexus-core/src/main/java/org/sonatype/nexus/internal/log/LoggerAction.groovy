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
package org.sonatype.nexus.internal.log

import org.apache.karaf.shell.commands.Argument
import org.apache.karaf.shell.commands.Command
import org.apache.karaf.shell.commands.Option
import org.apache.karaf.shell.console.AbstractAction
import org.sonatype.nexus.commands.Complete
import org.sonatype.nexus.log.LogManager
import org.sonatype.nexus.log.LoggerLevel

import javax.inject.Inject
import javax.inject.Named

/**
 * Action to set or display logger level.
 *
 * @since 3.0
 */
@Named
@Command(name='logger', scope = 'nexus', description = 'Set or display logger level')
class LoggerAction
  extends AbstractAction
{
  @Inject
  LogManager logManager

  @Option(name='-d', aliases = ['--delete'], description = 'Delete logger')
  Boolean delete

  @Option(name='-e', aliases = ['--effective'], description = 'Return effective logger level')
  Boolean effective

  // FIXME: Presently a strict flag is set on some Karaf stuff related to completion, and
  // FIXME: ... unless you use the logger-name completer, then the level completion will not get picked up
  // FIXME: ... unsure where, but looks like if a completer fails, all completers after it are ignored

  @Argument(name="name", index = 0, required = true, description = 'Logger name')
  @Complete('logger-name')
  String name

  @Argument(name="level", index = 1, description = 'Logger level')
  @Complete('auto')
  LoggerLevel level

  @Override
  protected def doExecute() {
    if (delete) {
      logManager.unsetLoggerLevel(name)
    }
    else if (level) {
      logManager.setLoggerLevel(name, level)
    }
    else {
      if (effective) {
        level = logManager.getLoggerEffectiveLevel(name)
      }
      else {
        level = logManager.getLoggerLevel(name)
      }

      if (level) {
        println "$name = $level"
      }
      else {
        println "$name is not set"
      }
    }
    return null
  }
}
