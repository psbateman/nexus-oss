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
 * Helpers to interact with **{@link NX.controller.State}** controller.
 *
 * @since 3.0
 */
Ext.define('NX.State', {
  singleton: true,
  requires: [
    'Ext.Version'
  ],
  mixins: {
    observable: 'Ext.util.Observable',
    logAware: 'NX.LogAware'
  },

  constructor: function (config) {
    var me = this;

    me.mixins.observable.constructor.call(me, config);

    me.addEvents(
        /**
         * Fires when any of application context values changes.
         *
         * @event changed
         * @param {NX.State} this
         */
        'changed'
    );
  },

  /**
   * @public
   * @returns {boolean} true, if browser is supported
   */
  isBrowserSupported: function () {
    return this.getValue('browserSupported') === true;
  },

  /**
   * @public
   * @param {boolean} value true, if browser is supported
   */
  setBrowserSupported: function (value) {
    this.setValue('browserSupported', value === true);
  },

  /**
   * @public
   * @returns {boolean} true, if license is required
   */
  requiresLicense: function () {
    return this.getValue('license', {})['required'] === true;
  },

  /**
   * @public
   * @returns {boolean} true, if license is installed
   */
  isLicenseInstalled: function () {
    return this.getValue('license', {})['installed'] === true;
  },

  /**
   * @public
   * @returns {Object} current user, if any
   */
  getUser: function () {
    return this.getValue('user');
  },

  /**
   * @public
   * @param {Object} [user] current user to be set
   * @returns {*}
   */
  setUser: function (user) {
    this.setValue('user', user);
  },

  /**
   * Return status.version
   *
   * @public
   * @returns {string}
   */
  getVersion: function() {
    return this.getValue('status')['version'];
  },

  /**
   * Returns major.minor parts of status.version.
   *
   * @public
   * @returns {string}
   */
  getVersionMajorMinor: function() {
    // Ext.Version doesn't fully support our version scheme, but the major.minor bits it handles fine
    var v = Ext.create('Ext.Version', this.getVersion());
    return v.getMajor() + '.' + v.getMinor();
  },

  /**
   * Return status.edition.
   *
   * @public
   * @returns {string}
   */
  getEdition: function() {
    return this.getValue('status')['edition'];
  },

  /**
   * Return whether or not we're receiving from the server.
   *
   * @returns {boolean}
   */
  isReceiving: function() {
    return this.getValue('receiving');
  },

  getValue: function (key, defaultValue) {
    return this.controller().getValue(key, defaultValue);
  },

  setValue: function (key, value) {
    this.controller().setValue(key, value);
  },

  setValues: function (values) {
    this.controller().setValues(values);
  },

  /**
   * @private
   * @returns {NX.controller.State}
   */
  controller: function () {
    return NX.getApplication().getStateController();
  }

});
