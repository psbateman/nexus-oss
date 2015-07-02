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
 * Metrics panel.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.support.Metrics', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-coreui-support-metrics',
  requires: [
    'Ext.chart.Chart',
    'Ext.data.ArrayStore',
    'NX.I18n'
  ],
  autoScroll: true,

  dockedItems: [{
    xtype: 'toolbar',
    dock: 'top',
    cls: 'nx-actions',
    items: [
      {
        xtype: 'button',
        text: NX.I18n.get('Support_Metrics_Download_Button'),
        tooltip: NX.I18n.get('Metrics_Download_Tooltip'),
        glyph: 'xf019@FontAwesome' /* fa-download */,
        action: 'download'
      },
      '-',
      {
        xtype: 'button',
        text: NX.I18n.get('Support_Metrics_Dump_Button'),
        tooltip: NX.I18n.get('Support_Metrics_Dump_Tooltip'),
        glyph: 'xf019@FontAwesome' /* fa-download */,
        action: 'threads'
      }
    ]
  }],

  items: {
    xtype: 'panel',
    ui: 'nx-inset',
    layout: 'column',
    defaults: {
      style: {
        margin: '0px 20px 20px 0px'
      }
    },

    items: [
      {
        xtype: 'panel',
        title: NX.I18n.get('Support_Metrics_MemoryUsage_Title'),
        frame: true,
        ui: 'nx-subsection',
        height: 240,
        width: 300,
        layout: 'fit',

        items: [
          {
            xtype: 'chart',
            itemId: 'memoryUsage',

            animate: false,
            insetPadding: 40,

            store: Ext.create('Ext.data.ArrayStore', {
              fields: ['value']
            }),

            axes: [
              {
                type: 'gauge',
                position: 'gauge',
                minimum: 0,
                maximum: 100,
                steps: 10
              }
            ],

            series: [
              {
                type: 'gauge',
                field: 'value',
                donut: 30,
                colorSet: ['#F49D10', '#ddd'],

                tips: {
                  trackMouse: true,
                  renderer: function (storeItem, item) {
                    this.setTitle('Memory used: ' + storeItem.get('value') + '%');
                  }
                }
              }
            ]
          }
        ]
      },
      {
        xtype: 'panel',
        title: NX.I18n.get('Support_Metrics_MemoryDistribution_Title'),
        frame: true,
        ui: 'nx-subsection',
        height: 240,
        width: 300,
        layout: 'fit',

        items: [
          {
            xtype: 'chart',
            itemId: 'memoryDist',
            animate: false,
            insetPadding: 20,
            theme: 'Green',

            store: Ext.create('Ext.data.ArrayStore', {
              fields: ['name', 'data']
            }),

            series: [
              {
                type: 'pie',
                angleField: 'data',
                showInLegend: true,

                tips: {
                  trackMouse: true,
                  renderer: function (storeItem, item) {
                    this.setTitle(storeItem.get('name') + ': ' + storeItem.get('data') + ' bytes');
                  }
                }
              }
            ],

            legend: {
              position: 'right',
              boxStrokeWidth: 0
            }
          }
        ]
      },
      {
        xtype: 'panel',
        title: NX.I18n.get('Support_Metrics_ThreadStates_Title'),
        frame: true,
        ui: 'nx-subsection',
        height: 240,
        width: 300,

        layout: 'fit',
        colspan: 2,

        items: [
          {
            xtype: 'chart',
            itemId: 'threadStates',
            animate: false,
            insetPadding: 20,
            theme: 'Base',

            store: Ext.create('Ext.data.ArrayStore', {
              fields: ['name', 'data']
            }),

            series: [
              {
                type: 'pie',
                angleField: 'data',
                showInLegend: true,

                tips: {
                  trackMouse: true,
                  renderer: function (storeItem, item) {
                    // name: count
                    this.setTitle(storeItem.get('name') + ': ' + storeItem.get('data'));
                  }
                }
              }
            ],

            legend: {
              position: 'right',
              boxStrokeWidth: 0
            }
          }
        ]
      }
    ]
  },

  /**
   * @public
   */
  setTotalData: function (data) {
    this.down('panel #memoryUsage').getStore().loadData(data);
  },

  /**
   * @public
   */
  setMemoryDistData: function (data) {
    this.down('panel #memoryDist').getStore().loadData(data);
  },

  /**
   * @public
   */
  setThreadStatesData: function (data) {
    this.down('panel #threadStates').getStore().loadData(data);
  }
});
