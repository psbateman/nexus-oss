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
package org.sonatype.nexus.httpclient.config;

import java.io.IOException;

import org.sonatype.sisu.goodies.common.Time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

/**
 * {@link Time} in seconds serializer.
 *
 * @since 3.0
 */
public class SecondsSerializer
    extends StdSerializer<Time>
{

  public SecondsSerializer() {
    super(Time.class);
  }

  @Override
  public void serialize(final Time value, final JsonGenerator jgen, final SerializerProvider provider)
      throws IOException
  {
    jgen.writeNumber(value.toSecondsI());
  }
}
