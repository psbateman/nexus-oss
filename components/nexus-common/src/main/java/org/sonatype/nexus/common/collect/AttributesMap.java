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
package org.sonatype.nexus.common.collect;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Container for [String -> Object] attributes.
 *
 * @since 3.0
 */
public class AttributesMap
  extends ComponentSupport
  implements Iterable<Entry<String,Object>>
{
  protected final Map<String, Object> backing;

  public AttributesMap(final Map<String, Object> backing) {
    this.backing = checkNotNull(backing);
  }

  public AttributesMap() {
    this(Maps.<String, Object>newHashMap());
  }

  /**
   * Expose the underlying backing for attributes.
   */
  public Map<String, Object> backing() {
    return backing;
  }

  /**
   * Coerce value to type.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  private <T> T coerce(final Object value, final TypeToken<T> type) {
    if (value != null) {
      // TODO: PropertyEditor coercion?
      log.trace("Coerce: {} -> {}", value, type);
      return (T) type.getRawType().cast(value);
    }
    return null;
  }

  /**
   * Get attribute value for given key.
   */
  @Nullable
  public Object get(final String key) {
    checkNotNull(key);
    return backing.get(key);
  }

  /**
   * Get attribute value for given key with default-value if value is null.
   */
  @Nullable
  public Object get(final String key, @Nullable final Object defaultValue) {
    Object value = get(key);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * Get attribute value.
   */
  @Nullable
  public <T> T get(final Class<T> type) {
    Object value = get(AttributeKey.get(type));
    return type.cast(value);
  }

  /**
   * Get attribute value or create attribute value if not set.
   *
   * @return Existing or newly created attribute value.
   */
  public <T> T getOrCreate(final Class<T> type) {
    T value = get(type);
    if (value == null) {
      try {
        // create new attribute value with accessible=true ctor to allow construction of private/package-private
        Constructor<T> ctor = type.getDeclaredConstructor();
        log.trace("Creating '{}' with constructor: {}", type, ctor);
        ctor.setAccessible(true);
        value = ctor.newInstance();
      }
      catch (Exception e) {
        throw Throwables.propagate(e);
      }
      set(type, value);
    }
    return value;
  }

  /**
   * Get attribute value with type-key with default-value if value is null.
   */
  @Nullable
  public <T> T get(final Class<T> type, @Nullable final T defaultValue) {
    T value = get(type);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * Get coerced attribute value for given key.
   */
  @Nullable
  public <T> T get(final String key, final TypeToken<T> type) {
    checkNotNull(type);
    Object value = get(key);
    return coerce(value, type);
  }

  /**
   * Get coerced attribute value for given key.
   */
  @Nullable
  public <T> T get(final String key, final Class<T> type) {
    checkNotNull(type);
    return get(key, TypeToken.of(type));
  }

  /**
   * Get coerced attribute value for given key with default-value if value is null.
   */
  @Nullable
  public <T> T get(final String key, final TypeToken<T> type, @Nullable final T defaultValue) {
    T value = get(key, type);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  /**
   * Get coerced attribute value for given key with default-value if value is null.
   */
  @Nullable
  public <T> T get(final String key, final Class<T> type, @Nullable final T defaultValue) {
    checkNotNull(type);
    return get(key, TypeToken.of(type), defaultValue);
  }

  /**
   * Allow customization of missing key message.
   */
  protected String missingKeyMessage(final String key) {
    return "Missing: " + key;
  }

  /**
   * Get required attribute value for given key.
   */
  public Object require(final String key) {
    Object value = get(key);
    checkState(value != null, missingKeyMessage(key));
    return value;
  }

  /**
   * Get required attribute value for given type-key.
   */
  public <T> T require(final Class<T> type) {
    T value = get(type);
    checkState(value != null, missingKeyMessage(AttributeKey.get(type)));
    return value;
  }

  /**
   * Require attribute value with coercion support.
   */
  public <T> T require(final String key, final TypeToken<T> type) {
    checkNotNull(type);
    Object value = require(key);
    return coerce(value, type);
  }

  /**
   * Require attribute value with coercion support.
   */
  public <T> T require(final String key, final Class<T> type) {
    checkNotNull(type);
    return require(key, TypeToken.of(type));
  }

  /**
   * Set keyed attribute value.
   */
  @Nullable
  public Object set(final String key, final @Nullable Object value) {
    checkNotNull(key);
    if (value == null) {
      return remove(key);
    }
    Object replaced = backing.put(key, value);
    if (log.isTraceEnabled()) {
      log.trace("Set: {}={} ({})", key, value, value.getClass().getName());
    }
    return replaced;
  }

  /**
   * Set type-keyed attribute value.
   */
  @Nullable
  public <T> Object set(final Class<T> type, final @Nullable T value) {
    return set(AttributeKey.get(type), value);
  }

  /**
   * Remove attribute for given key.
   */
  @Nullable
  public Object remove(final String key) {
    checkNotNull(key);
    Object removed = backing.remove(key);
    if (removed != null) {
      log.trace("Removed: {}", key);
    }
    return removed;
  }

  /**
   * Remove attribute for given type-key.
   */
  @Nullable
  public Object remove(final Class type) {
    return remove(AttributeKey.get(type));
  }

  /**
   * Check if attributes contains given key.
   */
  public boolean contains(final String key) {
    checkNotNull(key);
    return backing.containsKey(key);
  }

  /**
   * Check if attributes contains given type-key.
   */
  public boolean contains(final Class type) {
    return contains(AttributeKey.get(type));
  }

  /**
   * Return all attribute keys.
   */
  public Set<String> keys() {
    return backing.keySet();
  }

  /**
   * Return all attribute entries.
   */
  public Set<Entry<String,Object>> entries() {
    return backing.entrySet();
  }

  @Override
  public Iterator<Entry<String, Object>> iterator() {
    return backing.entrySet().iterator();
  }

  /**
   * Check if attributes contains any values.
   */
  public boolean isEmpty() {
    return backing.isEmpty();
  }

  /**
   * Returns the number of defined attributes.
   */
  public int size() {
    return backing.size();
  }

  /**
   * Clear all attributes.
   */
  public void clear() {
    backing.clear();
    log.trace("Cleared");
  }

  @Override
  public String toString() {
    return backing.toString();
  }
}
