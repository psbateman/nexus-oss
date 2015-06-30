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
 * User search box.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.user.UserSearchBox', {
  extend: 'Ext.form.field.Trigger',
  alias: 'widget.nx-coreui-user-searchbox',
  requires: [
    'Ext.util.KeyNav',
    'NX.I18n'
  ],

  emptyText: NX.I18n.get('User_UserList_Filter_EmptyText'),
  width: 320,
  submitValue: false,

  // TODO: Only show clear trigger if we have text
  trigger1Cls: 'nx-form-fa-times-circle-trigger',
  trigger2Cls: 'x-form-search-trigger',

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.callParent(arguments);

    me.addEvents(
        /**
         * @event search
         * Fires when a search trigger was pressed.
         * @param {NX.view.header.SearchBox} this search box
         * @param {String} search value
         */
        'search',

        /**
         * @event searchcleared
         * Fires when a search value had been cleared.
         * @param {NX.view.header.SearchBox} this search box
         */
        'searchcleared'
    );
  },

  /**
   * @override
   */
  initEvents: function() {
    var me = this;

    me.callParent();

    me.keyNav = new Ext.util.KeyNav(me.inputEl, {
      esc: {
        handler: me.clearSearch,
        scope: me,
        defaultEventAction: false
      },
      enter: {
        handler: me.onEnter,
        scope: me,
        defaultEventAction: false
      },
      scope: me,
      forceKeyDown: true
    });
  },

  /**
   * @private
   * Clear search.
   */
  onTrigger1Click: function() {
    var me = this;

    me.clearSearch();
  },

  /**
   * @private
   * Search on search trigger pressed.
   */
  onTrigger2Click: function() {
    var me = this;

    me.search(me.getValue());
  },

  /**
   * @private
   * Search on ENTER.
   */
  onEnter: function() {
    var me = this;

    me.search(me.getValue());
  },

  /**
   * @public
   * Search for value and fires a 'search' event.
   * @param value to search for
   */
  search: function(value) {
    var me = this;

    if (value !== me.getValue()) {
      me.setValue(value);
    }
    me.fireEvent('search', me, value);
  },

  /**
   * @public
   * Clears the search.
   */
  clearSearch: function() {
    var me = this;

    if (me.getValue()) {
      me.setValue(undefined);
    }
    me.fireEvent('searchcleared', me);
  }

});
