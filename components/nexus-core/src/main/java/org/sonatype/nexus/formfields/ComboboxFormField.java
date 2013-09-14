/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */

package org.sonatype.nexus.formfields;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A combo box {@link FormField}.
 *
 * @since 2.7
 */
public class ComboboxFormField<V>
    extends AbstractFormField<V>
    implements Selectable
{

  private String storePath;

  private String storeRoot;

  private String idMapping;

  private String nameMapping;

  public ComboboxFormField(final String id,
                           final String label,
                           final String helpText,
                           final boolean required,
                           final V initialValue)
  {
    super(id, label, helpText, required, null, initialValue);
    this.storePath = defaultStorePath();
    this.storeRoot = defaultStoreRoot();
  }

  public ComboboxFormField(final String id,
                           final String label,
                           final String helpText,
                           final boolean required)
  {
    this(id, label, helpText, required, null);
  }

  public ComboboxFormField(final String id,
                           final String label,
                           final String helpText)
  {
    this(id, label, helpText, FormField.OPTIONAL);
  }

  public ComboboxFormField(final String id,
                           final String label)
  {
    this(id, label, null);
  }

  @Override
  public String getType() {
    return "combobox";
  }

  @Override
  public String getStorePath() {
    return storePath;
  }

  @Override
  public String getStoreRoot() {
    return storeRoot;
  }

  @Override
  public String getIdMapping() {
    return idMapping;
  }

  @Override
  public String getNameMapping() {
    return nameMapping;
  }

  public ComboboxFormField<V> withStorePath(final String storePath) {
    this.storePath = checkNotNull(storePath);
    return this;
  }

  public ComboboxFormField<V> withStoreRoot(final String storeRoot) {
    this.storeRoot = storeRoot;
    return this;
  }

  public ComboboxFormField<V> withIdMapping(final String idMapping) {
    this.idMapping = idMapping;
    return this;
  }

  public ComboboxFormField<V> withNameMapping(final String nameMapping) {
    this.nameMapping = nameMapping;
    return this;
  }

  public ComboboxFormField<V> withId(final String id) {
    setId(id);
    return this;
  }

  public ComboboxFormField<V> witLabel(final String label) {
    setLabel(label);
    return this;
  }

  public ComboboxFormField<V> witHelpText(final String helpText) {
    setHelpText(helpText);
    return this;
  }

  public ComboboxFormField<V> withRegexValidation(final String regex) {
    setRegexValidation(regex);
    return this;
  }

  public ComboboxFormField<V> withRequired(final boolean required) {
    setRequired(required);
    return this;
  }

  public ComboboxFormField<V> optional() {
    return withRequired(FormField.OPTIONAL);
  }

  public ComboboxFormField<V> mandatory() {
    return withRequired(FormField.MANDATORY);
  }

  public ComboboxFormField<V> withInitialValue(final V value) {
    setInitialValue(value);
    return this;
  }

  protected String defaultStorePath() {
    return null;
  }

  protected String defaultStoreRoot() {
    return null;
  }

  public static String siestaStore(final String path) {
    checkArgument(path.startsWith("/"), "Path '%s' must start with slash ('/')", path);
    return "/service/siesta" + path;
  }

  public static String restlet1xStore(final String path) {
    checkArgument(path.startsWith("/"), "Path '%s' must start with slash ('/')", path);
    return "/service/local" + path;
  }

}
