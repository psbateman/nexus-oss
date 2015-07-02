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
 * Logging controller.
 *
 * @since 3.0
 * @see NX.util.log.Sink
 */
Ext.define('NX.controller.Logging', {
  extend: 'Ext.app.Controller',
  requires: [
    'Ext.util.MixedCollection',
    'NX.Log',
    'NX.util.log.StoreSink',
    'NX.util.log.ConsoleSink',
    'NX.util.log.RemoteSink'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  stores: [
    'LogEvent'
  ],

  /**
   * Array of sinks to receive events.
   *
   * @private
   * @property {Ext.util.MixedCollection}
   */
  sinks: Ext.create('Ext.util.MixedCollection'),

  /**
   * Logging threshold.
   *
   * @private
   * @property {String}
   */
  threshold: 'debug',

  /**
   * @override
   */
  init: function () {
    this.sinks.addAll({
      store: Ext.create('NX.util.log.StoreSink', this.getStore('LogEvent')),
      console: Ext.create('NX.util.log.ConsoleSink'),
      remote: Ext.create('NX.util.log.RemoteSink')
    });
  },

  /**
   * Attach to {@link NX.Log} helper.
   *
   * @override
   */
  onLaunch: function () {
    NX.Log.attach(this);
  },

  /**
   * Returns sink by name, or undefined.
   *
   * @public
   * @param {String} name
   */
  getSink: function(name) {
    return this.sinks.getByKey(name);
  },

  /**
   * Get the logging threshold.
   *
   * @public
   * @returns {String}
   */
  getThreshold: function () {
    return this.threshold;
  },

  /**
   * Set the logging threshold.
   *
   * @public
   * @param {String} threshold
   */
  setThreshold: function (threshold) {
    this.threshold = threshold;
  },

  /**
   * Mapping of {@link NX.model.LogLevel} weights.
   *
   * @private
   */
  levelWeights: {
    all: 1,
    trace: 2,
    debug: 3,
    info: 4,
    warn: 5,
    error: 6,
    off: 7
  },

  /**
   * Check if given level exceeds configured threshold.
   *
   * @private
   * @param {String} level
   * @return {Boolean}
   */
  exceedsThreshold: function (level) {
    return this.levelWeights[level] >= this.levelWeights[this.threshold];
  },

  /**
   * Record a log-event.
   *
   * @public
   * @param event
   */
  recordEvent: function (event) {
    // ignore events that do not exceed threshold
    if (!this.exceedsThreshold(event.level)) {
      return;
    }

    // pass events to all enabled sinks
    this.sinks.each(function (sink) {
      if (sink.enabled) {
        sink.receive(event);
      }
    });
  }
});
