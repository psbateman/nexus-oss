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
package org.sonatype.nexus.capability.condition;

import org.sonatype.nexus.capability.Condition;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * {@link NexusConditions} UTs.
 *
 * @since capabilities 2.0
 */
public class NexusConditionsTest
    extends TestSupport
{

  /**
   * active() factory method returns expected condition.
   */
  @Test
  public void active() {
    final NexusIsActiveCondition nexusIsActiveCondition = mock(NexusIsActiveCondition.class);
    final NexusConditions underTest = new NexusConditions(nexusIsActiveCondition);

    assertThat(
        underTest.active(),
        is(Matchers.<Condition>instanceOf(NexusIsActiveCondition.class))
    );
  }

}
