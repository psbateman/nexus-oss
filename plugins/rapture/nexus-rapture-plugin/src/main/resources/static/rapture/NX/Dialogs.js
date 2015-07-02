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
/*global Ext*/

/**
 * Helpers to show dialog boxes.
 *
 * @since 3.0
 */
Ext.define('NX.Dialogs', {
  singleton: true,
  requires: [
    'NX.I18n'
  ],

  /**
   * @public
   */
  showInfo: function (title, message, options) {
    options = options || {};

    // set default configuration
    Ext.applyIf(options, {
      title: title || NX.I18n.get('Dialogs_Info_Title'),
      msg: message,
      buttons: Ext.Msg.OK,
      icon: Ext.MessageBox.INFO,
      closable: false
    });

    Ext.Msg.show(options);
  },

  /**
   * @public
   */
  showError: function (title, message, options) {
    options = options || {};

    // set default configuration
    Ext.applyIf(options, {
      title: title || NX.I18n.get('Dialogs_Error_Title'),
      msg: message || NX.I18n.get('Dialogs_Error_Message'),
      buttons: Ext.Msg.OK,
      icon: Ext.MessageBox.ERROR,
      closable: false
    });

    Ext.Msg.show(options);
  },

  /**
   * @public
   */
  askConfirmation: function (title, message, onYesFn, options) {
    options = options || {};
    Ext.Msg.show({
      title: title,
      msg: message,
      buttons: Ext.Msg.YESNO,
      icon: Ext.MessageBox.QUESTION,
      closeable: false,
      animEl: options.animEl,
      fn: function (buttonName) {
        if (buttonName === 'yes' || buttonName === 'ok') {
          if (Ext.isDefined(onYesFn)) {
            onYesFn.call(options.scope);
          }
        }
      }
    });
  }

});
