/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/**
 * Analytics Event grid.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.licensing.AnalyticsEventList', {
  extend: 'Ext.grid.Panel',
  alias: 'widget.nx-coreui-analytics-event-list',

  store: 'AnalyticsEvent',

  columns: [
    {
      xtype: 'nx-iconcolumn',
      width: 36,
      iconVariant: 'x16',
      iconName: function(value, meta, record) {
        var type = record.get('type');
        switch (type) {
          case 'REST':
            return 'analyticsevent-rest';
          case 'Ext.Direct':
            return 'analyticsevent-ui';
          default:
            return 'analyticsevent-default';
        }
      }
    },
    {
      header: 'Type',
      dataIndex: 'type',
      flex: 1
    },
    {
      header: 'Timestamp',
      dataIndex: 'timestamp',
      flex: 1
    },
    {
      header: 'Duration',
      dataIndex: 'duration',
      flex: 1
    },
    {
      header: 'User',
      dataIndex: 'userId',
      flex: 1
    },
    {
      header: 'Attributes',
      dataIndex: 'attributes',
      flex: 3,
      renderer: function (value) {
        var text = '';
        Ext.Object.each(value, function (name, value) {
          if (text !== '') {
            text += ', ';
          }
          text += name + '=' + value;
        });
        return text;
      }
    }
  ],

  tbar: [
    {
      xtype: 'button',
      text: 'Clear',
      tooltip: 'Clear all event data',
      glyph: 'xf056@FontAwesome' /* fa-minus-circle */,
      action: 'clear',
      disabled: true
    },
    {
      xtype: 'button',
      text: 'Export',
      tooltip: 'Export and download event data',
      glyph: 'xf019@FontAwesome' /* fa-download */,
      action: 'export'
    },
    '-',
    {
      xtype: 'button',
      text: 'Submit',
      tooltip: 'Submit event data to Sonatype',
      glyph: 'xf023@FontAwesome' /* fa-lock */,
      action: 'submit',
      disabled: true
    }
  ],

  dockedItems: [
    {
      xtype: 'pagingtoolbar',
      store: 'AnalyticsEvent',
      dock: 'bottom',
      displayInfo: true,
      displayMsg: 'Displaying events {0} - {1} of {2}',
      emptyMsg: 'No events to display'
    }
  ],

  plugins: [
    {
      ptype: 'rowexpander',
      rowBodyTpl: new Ext.XTemplate(
          '<table style="padding: 5px;">',
          '<tpl for="this.attributes(values)">',
          '<tr>',
          '<td style="padding-right: 5px;"><b>{name}</b></td>',
          '<td>{value}</td>',
          '</tr>',
          '</tpl>',
          '</table>',
          {
            compiled: true,

            /**
             * Convert attributes field to array of name/value pairs for rendering in template.
             */
            attributes: function (values) {
              var result = [];
              Ext.iterate(values.attributes, function (name, value) {
                result.push({ name: name, value: value });
              });
              return result;
            }
          })
    },
    { ptype: 'gridfilterbox', emptyText: 'No analytics event matched criteria "$filter"' }
  ]

});
