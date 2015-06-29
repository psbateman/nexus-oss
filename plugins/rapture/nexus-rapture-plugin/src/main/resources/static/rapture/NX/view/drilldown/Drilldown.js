/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
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
 * The foundation class for new drilldowns. Extend this.
 *
 * @since 3.0
 */
Ext.define('NX.view.drilldown.Drilldown', {
  extend: 'Ext.container.Container',
  alias: 'widget.nx-drilldown',
  itemId: 'nx-drilldown',

  requires: [
    'NX.Icons'
  ],

  // List of masters to use (xtype objects)
  masters: null,

  // List of actions to use in the detail view
  actions: null,

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
      items = [];

    // Normalize the list of masters
    if (!me.masters) {
      me.masters = [];
    } else if (!Ext.isArray(me.masters)) {
      me.masters = [me.masters];
    }

    // Add the detail panel to the masters array
    if (me.detail) {
      // Use a custom detail panel
      me.masters.push(me.detail);
    }
    else {
      // Use the default tab panel
      me.masters.push(
        {
          xtype: 'nx-drilldown-details',
          ui: 'nx-drilldown-tabs',
          header: false,
          plain: true,

          layout: {
            type: 'vbox',
            align: 'stretch',
            pack: 'start'
          },

          tabs: Ext.clone(me.tabs),
          actions: Ext.isArray(me.actions) ? Ext.Array.clone(me.actions) : me.actions
        }
      );
    }

    // Stack all panels onto the items array
    for (var i = 0; i < me.masters.length; ++i) {
      items.push(me.createDrilldownItem(i, me.masters[i], undefined));
    }

    // Initialize this component’s items
    me.items = {
      xtype: 'container',

      defaults: {
        flex: 1
      },

      layout: {
        type: 'hbox',
        align: 'stretch'
      },

      items: items
    };

    me.callParent(arguments);
  },

  /**
   * @private
   * Create a new drilldown item
   */
  createDrilldownItem: function(index, browsePanel, createPanel) {
    var me = this;

    return {
      xtype: 'nx-drilldown-item',
      itemClass: NX.Icons.cls(me.iconName) + (index === 0 ? '-x32' : '-x16'),
      items: [
        {
          xtype: 'container',
          layout: 'fit',
          itemId: 'browse' + index,
          items: browsePanel
        },
        {
          xtype: 'container',
          layout: 'fit',
          itemId: 'create' + index,
          items: createPanel
        },
        {
          type: 'container',
          layout: 'fit',
          itemId: 'nothin' + index
        }
      ]
    }
  }
});
