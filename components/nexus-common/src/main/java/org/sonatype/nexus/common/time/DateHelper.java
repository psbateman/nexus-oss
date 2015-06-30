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
package org.sonatype.nexus.common.time;

import java.util.Date;

import javax.annotation.Nullable;

import org.joda.time.DateTime;

/**
 * Helper for {@link DateTime} and {@link Date} types.
 *
 * @since 3.0
 */
public class DateHelper
{
  @Nullable
  public static DateTime toDateTime(@Nullable final Date date) {
    return toDateTime(date, (DateTime) null);
  }

  @Nullable
  public static DateTime toDateTime(@Nullable final Date date, @Nullable final Date defaultValue) {
    return toDateTime(date, toDateTime(defaultValue));
  }

  @Nullable
  public static DateTime toDateTime(@Nullable final Date date, @Nullable final DateTime defaultValue) {
    if (date == null) {
      return defaultValue;
    }
    return new DateTime(date.getTime());
  }


  @Nullable
  public static Date toDate(@Nullable final DateTime dateTime) {
    return toDate(dateTime, (Date) null);
  }

  @Nullable
  public static Date toDate(@Nullable final DateTime dateTime, @Nullable DateTime defaultValue) {
    return toDate(dateTime, toDate(defaultValue));
  }

  @Nullable
  public static Date toDate(@Nullable final DateTime dateTime, @Nullable Date defaultValue) {
    if (dateTime == null) {
      return defaultValue;
    }
    return dateTime.toDate();
  }
}
