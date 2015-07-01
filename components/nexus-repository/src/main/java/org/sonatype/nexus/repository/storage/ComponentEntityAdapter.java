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
package org.sonatype.nexus.repository.storage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.OIndexNameBuilder;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static org.sonatype.nexus.repository.storage.StorageFacet.P_BUCKET;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_GROUP;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_NAME;
import static org.sonatype.nexus.repository.storage.StorageFacet.P_VERSION;

/**
 * {@link Component} entity-adapter.
 *
 * @since 3.0
 */
@Named
@Singleton
public class ComponentEntityAdapter
    extends MetadataNodeEntityAdapter<Component>
{
  public static final String DB_CLASS = new OClassNameBuilder()
      .type(Component.class)
      .build();

  private static final String I_BUCKET_GROUP_NAME_VERSION = new OIndexNameBuilder()
      .type(DB_CLASS)
      .property(P_BUCKET)
      .property(P_GROUP)
      .property(P_NAME)
      .property(P_VERSION)
      .build();

  @Inject
  public ComponentEntityAdapter(final BucketEntityAdapter bucketEntityAdapter) {
    super(DB_CLASS, bucketEntityAdapter);
  }

  @Override
  protected void defineType(final ODatabaseDocumentTx db, final OClass type) {
    super.defineType(type);
    type.createProperty(P_GROUP, OType.STRING);
    type.createProperty(P_NAME, OType.STRING).setMandatory(true).setNotNull(true);
    type.createProperty(P_VERSION, OType.STRING);

    ODocument metadata = db.newInstance()
        .field("ignoreNullValues", false)
        .field("mergeKeys", false);
    type.createIndex(I_BUCKET_GROUP_NAME_VERSION, INDEX_TYPE.UNIQUE.name(), null, metadata,
        new String[]{P_BUCKET, P_GROUP, P_NAME, P_VERSION});
  }

  @Override
  protected Component newEntity() {
    return new Component();
  }

  @Override
  protected void readFields(final ODocument document, final Component entity) {
    super.readFields(document, entity);

    String group = document.field(P_GROUP, OType.STRING);
    String name = document.field(P_NAME, OType.STRING);
    String version = document.field(P_VERSION, OType.STRING);

    entity.group(group);
    entity.name(name);
    entity.version(version);
  }

  @Override
  protected void writeFields(final ODocument document, final Component entity) {
    super.writeFields(document, entity);

    document.field(P_GROUP, entity.group());
    document.field(P_NAME, entity.name());
    document.field(P_VERSION, entity.version());
  }

}
