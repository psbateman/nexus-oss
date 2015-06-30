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
package org.sonatype.nexus.internal.capability.storage;

import java.util.Map;

import javax.annotation.Nullable;

import org.sonatype.nexus.orient.OClassNameBuilder;
import org.sonatype.nexus.orient.entity.CollectionEntityAdapter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link CapabilityStorageItem} entity adapter.
 *
 * @since 3.0
 */
public class CapabilityStorageItemEntityAdapter
  extends CollectionEntityAdapter<CapabilityStorageItem>
{
  public static final String DB_CLASS = new OClassNameBuilder()
      .prefix("capability")
      .type(CapabilityStorageItem.class)
      .build();

  public static final String P_VERSION = "version";

  public static final String P_TYPE = "type";

  public static final String P_ENABLED = "enabled";

  public static final String P_NOTES = "notes";

  public static final String P_PROPERTIES = "properties";

  public CapabilityStorageItemEntityAdapter() {
    super(DB_CLASS);
  }

  @Override
  protected void defineType(final OClass type) {
    type.createProperty(P_VERSION, OType.INTEGER);
    type.createProperty(P_TYPE, OType.STRING);
    type.createProperty(P_ENABLED, OType.BOOLEAN);
    type.createProperty(P_NOTES, OType.STRING);
    type.createProperty(P_PROPERTIES, OType.EMBEDDEDMAP);
  }

  @Override
  protected CapabilityStorageItem newEntity() {
    return new CapabilityStorageItem();
  }

  @Override
  protected void readFields(final ODocument document, final CapabilityStorageItem entity) {
    entity.setVersion((Integer) document.field(P_VERSION, OType.INTEGER));
    entity.setType((String) document.field(P_TYPE, OType.STRING));
    entity.setEnabled((Boolean) document.field(P_ENABLED, OType.BOOLEAN));
    entity.setNotes((String) document.field(P_NOTES, OType.STRING));

    Map<String,String> properties = document.field(P_PROPERTIES, OType.EMBEDDEDMAP);
    entity.setProperties(properties);
  }

  @Override
  protected void writeFields(final ODocument document, final CapabilityStorageItem entity) {
    document.field(P_VERSION, entity.getVersion());
    document.field(P_TYPE, entity.getType());
    document.field(P_ENABLED, entity.isEnabled());
    document.field(P_NOTES, entity.getNotes());
    document.field(P_PROPERTIES, entity.getProperties());
  }

  //
  // String-based ID helpers
  //

  @Nullable
  private ODocument findDocument(final ODatabaseDocumentTx db, final String id) {
    ORID rid = getRecordIdObfuscator().decode(getType(), id);
    return db.getRecord(rid);
  }

  @Nullable
  public CapabilityStorageItem read(final ODatabaseDocumentTx db, final String id) {
    checkNotNull(db);
    checkNotNull(id);

    ODocument document = findDocument(db, id);
    if (document != null) {
      return readEntity(document);
    }
    return null;
  }

  public boolean edit(final ODatabaseDocumentTx db, final String id, final CapabilityStorageItem entity) {
    checkNotNull(db);
    checkNotNull(id);
    checkNotNull(entity);

    ODocument document = findDocument(db, id);
    if (document != null) {
      writeEntity(document, entity);
      return true;
    }
    return false;
  }

  public boolean delete(final ODatabaseDocumentTx db, final String id) {
    checkNotNull(db);
    checkNotNull(id);

    ODocument document = findDocument(db, id);
    if (document != null) {
      db.delete(document);
      return true;
    }
    return false;
  }
}
