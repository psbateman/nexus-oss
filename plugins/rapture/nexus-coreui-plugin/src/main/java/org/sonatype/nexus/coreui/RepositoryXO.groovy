/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.coreui

import javax.validation.constraints.NotNull

import org.sonatype.nexus.validation.group.Create

import groovy.transform.ToString
import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.NotEmpty

/**
 * Repository exchange object.
 *
 * @since 3.0
 */
@ToString(includePackage = false, includeNames = true)
class RepositoryXO
{
  @NotBlank
  String name

  String type

  String format

  @NotBlank(groups = Create)
  String recipe

  @NotEmpty
  Map<String, Map<String, Object>> attributes

  String url

  RepositoryStatusXO status
}
