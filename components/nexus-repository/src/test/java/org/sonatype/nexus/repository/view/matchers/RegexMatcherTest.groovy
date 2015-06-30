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
package org.sonatype.nexus.repository.view.matchers

import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Request
import org.sonatype.sisu.litmus.testsupport.TestSupport

import org.junit.Test

import static org.mockito.Mockito.mock

/**
 * Tests for {@link RegexMatcher}.
 */
class RegexMatcherTest
  extends TestSupport
{
  private Context context(String path) {
    return new Context(mock(Repository.class), new Request.Builder().action('GET').path(path).build())
  }

  @Test
  void 'basic'() {
    def underTest = new RegexMatcher('foo.*bar')
    assert underTest.matches(context('foobar'))
    assert underTest.matches(context('fooooooobar'))
    assert !underTest.matches(context('foobarbaz'))
  }
}
