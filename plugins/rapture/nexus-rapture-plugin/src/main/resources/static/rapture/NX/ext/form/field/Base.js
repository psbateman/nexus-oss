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
 * **{@link Ext.form.field.Base}** override, that changes default label width.
 *
 * @since 3.0
 */
Ext.define('NX.ext.form.field.Base', {
  override: 'Ext.form.field.Base',

  width: 600,
  labelAlign: 'top',
  labelStyle: 'font-weight: bold;',
  msgTarget: 'under',

  /**
   * @cfg {boolean} [hideIfUndefined=false]
   * If field should auto hide in case it has no value. Functionality applies only for read only field.
   */
  hideIfUndefined: false,

  initComponent: function () {
    var me = this;

    if (me.helpText) {
      me.afterLabelTpl = '<span style="font-size: 10px;">' + me.helpText + '</span>';
    }

    me.callParent(arguments);
  },

  setValue: function (value) {
    var me = this;
    me.callParent(arguments);
    if (me.readOnly && me.hideIfUndefined) {
      if (value) {
        me.show();
      }
      else {
        me.hide();
      }
    }
  }

});
