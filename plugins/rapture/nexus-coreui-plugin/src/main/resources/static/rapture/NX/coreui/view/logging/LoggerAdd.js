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
 * Add logger window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.logging.LoggerAdd', {
  extend: 'NX.view.AddPanel',
  alias: 'widget.nx-coreui-logger-add',
  requires: [
    'NX.I18n'
  ],

  defaultFocus: 'name',

  settingsForm: {
    xtype: 'nx-settingsform',
    items: [
      {
        xtype: 'textfield',
        name: 'name',
        itemId: 'name',
        fieldLabel: NX.I18n.get('Logging_LoggerAdd_Name_FieldLabel')
      },
      {
        xtype: 'combo',
        name: 'level',
        fieldLabel: NX.I18n.get('Logging_LoggerAdd_Level_FieldLabel'),
        editable: false,
        value: 'INFO',
        store: [
          ['TRACE', NX.I18n.get('Logging_LoggerList_Level_TraceItem')],
          ['DEBUG', NX.I18n.get('Logging_LoggerList_Level_DebugItem')],
          ['INFO', NX.I18n.get('Logging_LoggerList_Level_InfoItem')],
          ['WARN', NX.I18n.get('Logging_LoggerList_Level_WarnItem')],
          ['ERROR', NX.I18n.get('Logging_LoggerList_Level_ErrorItem')],
          ['OFF', NX.I18n.get('Logging_LoggerList_Level_OffItem')],
          ['DEFAULT', NX.I18n.get('Logging_LoggerList_Level_DefaultItem')]
        ],
        queryMode: 'local'
      }
    ],
    buttons: [
      { text: NX.I18n.get('Logging_LoggerList_New_Button'), action: 'add', formBind: true, ui: 'nx-primary' },
      { text: NX.I18n.get('Add_Cancel_Button'), handler: function () {
        this.up('nx-drilldown').showChild(0, true);
      }}
    ]
  }

});
