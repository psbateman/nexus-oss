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
 * About window.
 *
 * @since 3.0
 */
Ext.define('NX.view.AboutWindow', {
  extend: 'Ext.window.Window',
  alias: 'widget.nx-aboutwindow',
  requires: [
    'NX.I18n',
    'NX.Icons',
    'NX.State',
    'NX.util.Url'
  ],

  layout: {
    type: 'vbox',
    align: 'stretch'
  },

  autoShow: true,
  modal: true,
  constrain: true,
  width: 640,
  height: 480,

  title: NX.I18n.get('AboutWindow_Title'),

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {
        layout: {
          type: 'hbox',
          align: 'stretch'
        },
        bodyStyle: {
          backgroundColor: '#F4F4F4'
        },
        items: [
          {
            xtype: 'component',
            margin: '10 10 10 10',
            html: NX.Icons.img('nexus', 'x100')
          },
          {
            xtype: 'nx-info',
            itemId: 'aboutInfo',
            flex: 1
          }
        ]
      },
      {
        xtype: 'tabpanel',
        ui: 'nx-light',
        flex: 1,
        items: [
          {
            title: NX.I18n.get('AboutWindow_About_Title'),
            xtype: 'uxiframe',
            src: NX.util.Url.urlOf('/COPYRIGHT.html')
          },
          {
            title: NX.I18n.get('AboutWindow_License_Tab'),
            xtype: 'uxiframe',
            src: NX.util.Url.urlOf('/LICENSE.html')
          }
        ]
      }
    ];

    me.buttons = [
      { text: NX.I18n.get('AboutWindow_Close_Button'), action: 'close', ui: 'nx-primary', handler: function () { me.close(); }}
    ];
    me.buttonAlign = 'left';

    me.callParent(arguments);

    // populate initial details
    me.down('#aboutInfo').showInfo({
      'Version': NX.State.getVersion(),
      'Edition': NX.State.getEdition()
    });
  }

});
