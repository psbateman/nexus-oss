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
/*global Ext, NX*/

/**
 * Store {@link NX.util.log.Sink} into {@link NX.store.LogEvent} store.
 *
 * @since 3.0
 */
Ext.define('NX.util.log.StoreSink', {
  extend: 'NX.util.log.Sink',
  singleton: true,
  requires: [
    'NX.Assert'
  ],

  /**
   * Reference to the event store.
   *
   * @private
   * @property {NX.store.LogEvent}
   */
  store: undefined,

  /**
   * Maximum records to retain in the store.
   *
   * @property {Number}
   */
  maxSize: 200,

  /**
   * @public
   * @param {NX.store.LogEvent} store
   */
  attach: function (store) {
    this.store = store;
  },

  /**
   * @public
   * @param {Number} maxSize
   */
  setMaxSize: function (maxSize) {
    this.maxSize = maxSize;

    // log here should induce shrinkage, nothing more to do
    this.logDebug('Max size:', maxSize);
  },

  /**
   * Returns the size of the store, irregardless of filtering.
   *
   * @private
   * @returns {Number}
   */
  size: function () {
    return this.store.snapshot ? this.store.snapshot.getCount() : this.store.getCount();
  },

  /**
   * Array of ordered records for shrinking.
   *
   * @private
   * @property {NX.model.LogEvent[]}
   */
  records: [],

  /**
   * @override
   */
  receive: function (event) {
    //<if assert>
    NX.Assert.assert(this.store, 'Store not attached');
    //</if>

    var record = this.store.add(event)[0]; // only 1 record, pick off first

    // maybe shrink
    this.shrink();

    // track records for shrinkage
    this.records.push(record);
  },

  /**
   * Shrink the store after we breach maximum size.
   *
   * @private
   */
  shrink: function () {
    // calculate number of records to purge
    var remove = this.size() - this.maxSize;

    // maybe purge records
    if (remove > 0) {
      var purged = this.records.splice(0, remove);
      this.store.remove(purged);
    }
  }
});
