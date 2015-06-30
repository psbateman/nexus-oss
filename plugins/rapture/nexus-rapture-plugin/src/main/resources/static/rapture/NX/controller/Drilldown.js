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
 * Abstract Master/Detail controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Drilldown', {
  extend: 'Ext.app.Controller',
  requires: [
    'NX.Conditions',
    'NX.Dialogs',
    'NX.Bookmarks',
    'NX.view.drilldown.Drilldown',
    'NX.view.drilldown.Item'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },
  views: [
    'drilldown.Drilldown',
    'drilldown.Details'
  ],

  permission: undefined,

  /**
   * @protected
   * Get the human-readable name of a model
   */
  getDescription: Ext.emptyFn,

  /**
   * @override
   * An array of xtypes which represent the masters available to this drilldown
   */
  masters: null,

  /**
   * @cfg {Function} optional function to be called on delete
   */
  deleteModel: undefined,

  onLaunch: function () {
    var me = this;
    me.getApplication().getIconController().addIcons({
      'drilldown-info': {
        file: 'information.png',
        variants: ['x16', 'x32']
      },
      'drilldown-warning': {
        file: 'warning.png',
        variants: ['x16', 'x32']
      }
    });
  },

  /**
   * @override
   */
  init: function () {
    var me = this,
        componentListener = {};

    // Normalize lists into an array
    if (!me.masters) {
      me.masters = [];
    }

    // Add event handlers to each list
    for (var i = 0; i < me.masters.length; ++i) {
      componentListener[me.masters[i]] = {
        selection: me.onSelection,
        cellclick: me.onCellClick
      };
    }

    // Drilldown
    componentListener[me.masters[0] + ' ^ nx-drilldown'] = {
      syncsize: me.syncSizeToOwner
    };

    // New button
    componentListener[me.masters[0] + ' ^ nx-drilldown button[action=new]'] = {
      afterrender: me.bindNewButton
    };

    // Delete button
    componentListener[me.masters[0] + ' ^ nx-drilldown button[action=delete]'] = {
      afterrender: me.bindDeleteButton,
      click: me.onDelete
    };

    // Back button
    componentListener[me.masters[0] + ' ^ nx-drilldown nx-addpanel button[action=back]'] = {
      click: function() {
        me.showChild(0, true);
      }
    };

    me.listen({
      component: componentListener,
      controller: {
        '#Bookmarking': {
          navigate: me.navigateTo
        },
        '#Refresh': {
          refresh: me.reselect
        }
      }
    });

    if (me.icons) {
      me.getApplication().getIconController().addIcons(me.icons);
    }
    if (me.features) {
      me.getApplication().getFeaturesController().registerFeature(me.features, me);
    }
  },

  /**
   * @private
   */
  getDrilldown: function() {
    return Ext.ComponentQuery.query('#nx-drilldown')[0];
  },

  /**
   * @private
   */
  getDrilldownItems: function() {
    return Ext.ComponentQuery.query('nx-drilldown-item');
  },

  /**
   * @private
   */
  getDrilldownDetails: function() {
    return Ext.ComponentQuery.query('nx-drilldown-details')[0];
  },

  /**
   * @public
   */
  reselect: function () {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master');

    if (lists.length) {
      me.navigateTo(NX.Bookmarks.getBookmark());
    }
  },

  /**
   * @private
   * When a list item is clicked, display the new view and update the bookmark
   */
  onCellClick: function(list, td, cellIndex, model, tr, rowIndex, e) {
    var me = this,
      index = Ext.ComponentQuery.query('nx-drilldown-master').indexOf(list.up('grid'));

    //if the cell target is a link, let it do it's thing
    if(e.getTarget('a')) {
      return false;
    }
    me.loadView(index + 1, true, model);
  },

  /**
   * @private
   * A model changed, focus on the new row and update the name of the related drilldown
   */
  onModelChanged: function (index, model) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        feature = me.getFeature();

    lists[index].getSelectionModel().select([model], false, true);
    me.setItemName(index + 1, me.getDescription(model));
  },

  /**
   * @public
   * Make the detail view appear
   *
   * @param index The zero-based view to load
   * @param animate Whether to animate the panel into view
   * @param model An optional record to select
   */
  loadView: function (index, animate, model) {
    var me = this,
      lists = Ext.ComponentQuery.query('nx-drilldown-master');

    // Don’t load the view if the feature is not ready
    if (!me.getFeature()) {
      return;
    }

    // Model specified, select it in the previous list
    if (model && index > 0) {
      lists[index - 1].fireEvent('selection', lists[index - 1], model);
      me.onModelChanged(index - 1, model);
    }
    // Set all child bookmarks
    for (var i = 0; i <= index; ++i) {
      me.setItemBookmark(i, NX.Bookmarks.fromSegments(NX.Bookmarks.getBookmark().getSegments().slice(0, i + 1)), me);
    }

    // Show the next view in line
    me.showChild(index, animate);
    me.bookmark(model);
  },

  /**
   * @public
   * Make the create wizard appear
   *
   * @param index The zero-based step in the create wizard
   * @param animate Whether to animate the panel into view
   * @param cmp An optional component to load
   */
  loadCreateWizard: function (index, animate, cmp) {
    var me = this;

    // Reset all non-root bookmarks
    for (var i = 1; i <= index; ++i) {
      me.setItemBookmark(i, null);
    }

    // Show the specified step in the wizard
    me.showCreateWizard(index, animate, cmp);
  },

  /**
   * @private
   * Bookmark specified model / selected tab.
   */
  bookmark: function (model) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        bookmark = NX.Bookmarks.getBookmark().getSegments(),
        segments = [],
        index = 0;

    // Add the root element of the bookmark
    segments.push(bookmark.shift());

    // Find all parent models and add them to the bookmark array
    while (index < lists.length && model && !lists[index].getView().getNode(model)) {
      segments.push(bookmark.shift());
      ++index;
    }

    // Add the currently selected model to the bookmark array
    if (model) {
      segments.push(encodeURIComponent(model.getId()));
    }

    // Set the bookmark
    NX.Bookmarks.bookmark(NX.Bookmarks.fromSegments(segments), me);
  },

  /**
   * @public
   * @param {NX.Bookmark} bookmark to navigate to
   */
  navigateTo: function (bookmark) {
    var me = this,
        feature = me.getFeature(),
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        list_ids = bookmark.getSegments().slice(1),
        index, list_ids, modelId, store;

    if (feature && lists.length && list_ids.length) {
      //<if debug>
      me.logDebug('Navigate to: ' + bookmark.getSegments().join(':'));
      //</if>

      modelId = decodeURIComponent(list_ids.pop());
      index = list_ids.length;
      store = lists[index].getStore();

      if (store.isLoading()) {
        // The store hasn’t yet loaded, load it when ready
        me.mon(store, 'load', function() {
          me.selectModel(index, modelId);
          me.mun(store, 'load');
        });
      } else {
        me.selectModel(index, modelId);
      }
    } else {
      me.loadView(0, false);
    }
  },

  /**
   * @private
   * @param index of the list which owns the model
   * @param model to select
   */
  selectModel: function (index, modelId) {
    var me = this,
        lists = Ext.ComponentQuery.query('nx-drilldown-master'),
        store = lists[index].getStore(),
        model;

    // getById() throws an error if a model ID is found, but not cached, check for content first
    if (store.getCount()) {
      model = store.getById(modelId);
      lists[index].fireEvent('selection', lists[index], model);
      me.onModelChanged(index, model);
      me.loadView(index + 1, false, model);
    }
  },

  /**
   * @private
   */
  onDelete: function () {
    var me = this,
        selection = Ext.ComponentQuery.query('nx-drilldown-master')[0].getSelectionModel().getSelection(),
        description;

    if (Ext.isDefined(selection) && selection.length > 0) {
      description = me.getDescription(selection[0]);
      NX.Dialogs.askConfirmation('Confirm deletion?', description, function () {
        me.deleteModel(selection[0]);

        // Reset the bookmark
        NX.Bookmarks.bookmark(NX.Bookmarks.fromToken(NX.Bookmarks.getBookmark().getSegment(0)));
      }, {scope: me});
    }
  },

  /**
   * @protected
   * Enable 'New' when user has 'create' permission.
   */
  bindNewButton: function (button) {
    var me = this;
    button.mon(
        NX.Conditions.isPermitted(me.permission + ':create'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission.
   */
  bindDeleteButton: function (button) {
    var me = this;
    button.mon(
        NX.Conditions.isPermitted(me.permission + ':delete'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * Create an event handler so the panel will resize correctly when the window does
   *
   * @override
   */
  /*bindDrilldownPanel: function (drilldown) {
    var me = this;

    me.currentIndex = 0;

    if (drilldown.ownerCt) {
      me.relayEvents(drilldown.ownerCt, ['resize', 'afterlayout']);
      me.on({
        resize: me.syncSizeToOwner,
        afterlayout: me.syncSizeToOwner
      });
    }
  },*/

  // Constants which represent card indexes
  BROWSE_INDEX: 0,
  CREATE_INDEX: 1,
  BLANK_INDEX: 2,

  /**
   * @private
   * Given N drilldown items, this panel should have a width of N times the current screen width
   */
  syncSizeToOwner: function () {
    var me = this,
      owner = me.getDrilldown().ownerCt.body.el,
      container = me.getDrilldown().down('container');

    container.setSize(owner.getWidth() * container.items.length, owner.getHeight());
    me.slidePanels(me.currentIndex, false);
  },

  /**
   * @public
   * Shift this panel to display the referenced step in the create wizard
   *
   * @param index The index of the create wizard to display
   * @param animate Set to “true” if the view should slide into place, “false” if it should just appear
   * @param cmp An optional component to load into the panel
   */
  showCreateWizard: function (index, animate, cmp) {
    var me = this,
      drilldown = me.getDrilldown(),
      items = me.padItems(index), // Pad the drilldown
      createContainer;

    // Add a component to the specified drilldown item (if specified)
    if (cmp) {
      createContainer = drilldown.down('#create' + index);
      createContainer.removeAll();
      createContainer.add(cmp);
    }

    // Show the proper card
    items[index].setCardIndex(me.CREATE_INDEX);

    me.slidePanels(index, animate);
  },

  /**
   * @public
   * Shift this panel to display the referenced master or detail panel
   *
   * @param index The index of the master/detail panel to display
   * @param animate Set to “true” if the view should slide into place, “false” if it should just appear
   */
  showChild: function (index, animate) {
    var me = this,
      items = me.getDrilldownItems(),
      item = items[index],
      createContainer;

    // Show the proper card
    item.setCardIndex(me.BROWSE_INDEX);

    // Destroy any create wizard panels
    for (var i = 0; i < items.length; ++i) {
      createContainer = items[i].down('#create' + i);
      createContainer.removeAll();
    }

    me.slidePanels(index, animate);
  },

  /**
   * @private
   * Hide all except the specified panel. Focus on a default form field, if available.
   *
   * This is needed to restrict focus to the visible panel only.
   */
  hideAllExceptAndFocus: function (index) {
    var me = this,
      items = me.getDrilldownItems(),
      form;

    // Hide everything that’s not the specified panel, reset forms, etc…
    for (var i = 0; i < items.length; ++i) {
      if (i != index) {
        items[i].getLayout().setActiveItem(me.BLANK_INDEX);
      }

      // Reset forms and filters on successive drilldown items
      if (i > index) {
        Ext.each(items[i].query('nx-settingsform'), function(panel) {
          if (panel.getForm().isDirty()) {
            panel.getForm().reset();
          }
        });
      }
    }

    // Set focus on the default field (if available) or the panel itself
    form = items[index].down('nx-addpanel[defaultFocus]');
    if (form) {
      form.down('[name=' + form.defaultFocus + ']').focus();
    } else {
      me.getDrilldown().focus();
    }
  },

  /**
   * @private
   * Slide the drilldown to reveal the specified panel
   */
  slidePanels: function (index, animate) {
    var me = this,
      drilldown = me.getDrilldown(),
      feature = drilldown.up('nx-feature-content'),
      items = me.getDrilldownItems(),
      item = items[index],
      createContainer;

    // Destroy any create wizard panels after current
    for (var i = index + 1; i < items.length; ++i) {
      createContainer = items[i].down('#create' + i);
      createContainer.removeAll();
    }

    if (item && item.el) {

      // Restore the current card
      items[index].getLayout().setActiveItem(items[index].cardIndex);

      // Hack to suppress resize events until the animation is complete
      if (animate) {
        drilldown.ownerCt.suspendEvents(false);
        setTimeout(function () {
          drilldown.ownerCt.resumeEvents();
          me.hideAllExceptAndFocus(index);
        }, 300);
      } else {
        me.hideAllExceptAndFocus(index);
      }

      // Slide the requested panel into view
      var left = feature.el.getX() - (index * feature.el.getWidth());
      if (animate) {
        drilldown.animate({
          easing: 'easeInOut',
          duration: 200,
          to: {
            x: left
          }
        });
      } else {
        drilldown.setX(left, false);
      }
      me.currentIndex = index;

      // Update the breadcrumb
      me.refreshBreadcrumb();
      me.resizeBreadcrumb();
    }
  },

  /**
   * @private
   * Pad the number of items in this drilldown to the specified index
   */
  padItems: function (index) {
    var me = this,
      drilldown = me.getDrilldown(),
      items = me.getDrilldownItems(),
      itemContainer;

    // Create new drilldown items (if needed)
    if (index > items.length - 1) {
      itemContainer = drilldown.down('container');

      // Create empty panels if index > items.length
      for (var i = items.length; i <= index; ++i) {
        itemContainer.add(drilldown.createDrilldownItem(i, undefined, undefined));
      }

      // Resize the panel
      me.syncSizeToOwner();
    }

    return me.getDrilldownItems();
  },

  /**
   * @private
   * Update the breadcrumb based on the itemName and itemClass of drilldown items
   */
  refreshBreadcrumb: function() {
    var me = this,
      content = me.getDrilldown().up('#feature-content'),
      root = content.down('#feature-root'),
      breadcrumb = content.down('#breadcrumb'),
      items = me.getDrilldownItems();

    if (me.currentIndex == 0) {
      // Feature's home page, no breadcrumb required
      breadcrumb.hide();
      root.show();
    } else {
      breadcrumb.removeAll();

      // Make a breadcrumb (including icon and 'home' link)
      breadcrumb.add(
        {
          xtype: 'button',
          scale: 'large',
          ui: 'nx-drilldown',
          text: content.currentTitle,
          handler: function() {
            me.slidePanels(0, true);

            // Set the bookmark
            var bookmark = items[0].itemBookmark;
            if (bookmark) {
              NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
            }
          }
        }
      );

      // Create the rest of the links
      for (var i = 1; i <= me.currentIndex && i < items.length; ++i) {
        breadcrumb.add([
          // Separator
          {
            xtype: 'label',
            cls: 'nx-breadcrumb-separator',
            text: '/'
          },
          {
            xtype: 'image',
            height: 16,
            width: 16,
            cls: 'nx-breadcrumb-icon ' + items[i].itemClass
          },

          // Create a closure within a closure to decouple 'i' from the current context
          (function(j) {
            return {
              xtype: 'button',
              scale: 'medium',
              ui: 'nx-drilldown',
              disabled: (i === me.currentIndex ? true : false), // Disabled if it’s the last item in the breadcrumb
              text: items[j].itemName,
              handler: function() {
                var bookmark = items[j].itemBookmark;
                if (bookmark) {
                  NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
                }
                me.slidePanels(j, true);
              }
            }
          })(i)
        ]);
      }

      root.hide();
      breadcrumb.show();
    }
  },

  /*
   * @private
   * Resize the breadcrumb, truncate individual elements with ellipses as needed
   */
  resizeBreadcrumb: function() {
    var me = this,
      padding = 60, // Prevent truncation from happening too late
      parent = me.getDrilldown().ownerCt,
      breadcrumb = me.getDrilldown().up('#feature-content').down('#breadcrumb'),
      buttons, availableWidth, minimumWidth;

    // Is the breadcrumb clipped?
    if (parent && breadcrumb.getWidth() + padding > parent.getWidth()) {

      // Yes. Take measurements and get a list of buttons sorted by length (longest first)
      buttons = breadcrumb.query('button').splice(1);
      availableWidth = parent.getWidth();

      // What is the width of the breadcrumb, sans buttons?
      minimumWidth = breadcrumb.getWidth() + padding;
      for (var i = 0; i < buttons.length; ++i) {
        minimumWidth -= buttons[i].getWidth();
      }

      // Reduce the size of the longest button, until all buttons fit in the specified width
      me.reduceButtonWidth(buttons, availableWidth - minimumWidth);
    }
  },

  /*
   * @private
   * Reduce the width of a set of buttons, longest first, to a specified width
   *
   * @param buttons The list of buttons to resize
   * @param width The desired resize width (sum of all buttons)
   * @param minPerButton The minimum to resize each button (until all buttons are at this minimum)
   */
  reduceButtonWidth: function(buttons, width, minPerButton) {
    var me = this,
      currentWidth = 0,
      setToWidth;

    // Sort the buttons by width
    buttons = buttons.sort(function(a,b) { return b.getWidth() - a.getWidth() });

    // Calculate the current width of the buttons
    for (var i = 0; i < buttons.length; ++i) {
      currentWidth += buttons[i].getWidth();
    }

    // Find the next button to resize
    for (var i = 0; i < buttons.length; ++i) {

      // Shorten the longest button
      if (i < buttons.length - 1 && buttons[i].getWidth() > buttons[i+1].getWidth()) {

        // Will resizing this button make it fit?
        if (currentWidth - (buttons[i].getWidth() - buttons[i+1].getWidth()) <= width) {

          // Yes.
          setToWidth = width;
          for (var j = i + 1; j < buttons.length; ++j) {
            setToWidth -= buttons[j].getWidth();
          }
          buttons[i].setWidth(setToWidth);

          // Exit the algorithm
          break;
        }
        else {
          // No. Set the width of this button to that of the next button, and re-run the algorithm.
          buttons[i].setWidth(buttons[i+1].getWidth());
          me.reduceButtonWidth(buttons, width, minPerButton);
        }
      }
      else {
        // All buttons are the same length, shorten all by the same length
        setToWidth = Math.floor(width / buttons.length);
        for (var j = 0; j < buttons.length; ++j) {
          buttons[j].setWidth(setToWidth);
        }

        // Exit the algorithm
        break;
      }
    }
  },

  /**
   * @public
   * Set the name of the referenced drilldown item
   */
  setItemName: function (index, text) {
    var me = this,
      items = me.padItems(index);

    items[index].setItemName(text);
  },

  /**
   * @public
   * Set the icon class of the referenced drilldown item
   */
  setItemClass: function (index, cls) {
    var me = this,
      items = me.padItems(index);

    items[index].setItemClass(cls);
  },

  /**
   * @public
   * Set the bookmark of the breadcrumb segment associated with the referenced drilldown item
   */
  setItemBookmark: function (index, bookmark, scope) {
    var me = this,
      items = me.padItems(index);

    items[index].setItemBookmark(bookmark, scope);
  },

  /**
   * @public
   */
  showInfo: function (message) {
    this.getDrilldownDetails().showInfo(message);
  },

  /**
   * @public
   */
  clearInfo: function () {
    this.getDrilldownDetails().clearInfo();
  },

  /**
   * @public
   */
  showWarning: function (message) {
    this.getDrilldownDetails().showWarning(message);
  },

  /**
   * @public
   */
  clearWarning: function () {
    this.getDrilldownDetails().clearWarning();
  },

  /**
   * Add a tab to the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  addTab: function (tab) {
    var me = this;
    if (!me.detail) {
      me.getDrilldownDetails().addTab(tab);
    }
  },

  /**
   * Remove a panel from the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  removeTab: function (tab) {
    var me = this;
    if (!me.detail) {
      me.getDrilldownDetails().removeTab(tab);
    }
  }

});