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
 * Controls forms marked with settingsForm = true by adding save/discard/refresh functionality using form configured
 * api.
 *
 * @since 3.0
 */
Ext.define('NX.controller.SettingsForm', {
  extend: 'Ext.app.Controller',
  requires: [
    'Ext.ComponentQuery',
    'NX.Messages'
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.onRefresh
        }
      },
      component: {
        'form[settingsForm=true]': {
          afterrender: me.loadForm,
          load: me.loadForm
        },
        'form[settingsForm=true][editableCondition]': {
          afterrender: me.bindEditableCondition
        },
        'form[settingsForm=true][settingsFormSubmit=true] button[action=add]': {
          click: me.submitForm
        },
        'form[settingsForm=true][settingsFormSubmit=true] button[action=save]': {
          click: me.submitForm
        },
        'form[settingsForm=true] field[bindGroup]': {
          validitychange: me.updateEnableState
        }
      }
    });
  },

  /**
   * @private
   */
  onRefresh: function () {
    var me = this,
        forms = Ext.ComponentQuery.query('form[settingsForm=true]');

    if (forms) {
      Ext.each(forms, function (form) {
        me.loadForm(form);
      });
    }
  },

  /**
   * Loads the form if form's api load function is defined.
   *
   * @private
   */
  loadForm: function (form, options) {
    if (!form.isDestroyed && form.rendered) {
      if (form.api && form.api.load) {
        // Load the form
        form.load(Ext.applyIf(options || {}, {
          waitMsg: form.settingsFormLoadMessage,
          success: function (basicForm, action) {
            // Form is valid
            form.isValid();

            form.fireEvent('loaded', form, action);
          },
          failure: function (basicForm, action) {
            form.isValid();
          }
        }));
      }
      else {
        form.isValid();
      }
    }
  },

  /**
   * @private
   * Submits the form containing the button, if form's api submit function is defined.
   */
  submitForm: function (button) {
    var me = this,
        form = button.up('form');

    if (form.api && form.api.submit) {
      form.submit({
        waitMsg: form.settingsFormSubmitMessage,
        success: function (basicForm, action) {
          var title = me.getSettingsFormSuccessMessage(form, action);
          if (title) {
            NX.Messages.add({ text: title, type: 'success' });
          }
          form.fireEvent('submitted', form, action);
          me.loadForm(form);
        }
      });
    }
  },

  /**
   * Calculates title based on form's {NX.view.SettingsForm#getSettingsFormSuccessMessage}.
   *
   * @private
   * @param {NX.view.SettingsForm} form
   * @param {Ext.form.action.Action} action
   */
  getSettingsFormSuccessMessage: function (form, action) {
    var title;

    if (form.settingsFormSuccessMessage) {
      if (Ext.isFunction(form.settingsFormSuccessMessage)) {
        title = form.settingsFormSuccessMessage(action.result.data);
      }
      else {
        title = form.settingsFormSuccessMessage.toString();
      }
      title = title.replace(/\$action/, action.type.indexOf('submit') > -1 ? 'updated' : 'refreshed');
    }
    return title;
  },

  /**
   * Toggle editable on settings form hen editable condition is satisfied (if specified).
   *
   * @private
   * @param {NX.view.SettingsForm} form
   */
  bindEditableCondition: function (form) {
    if (Ext.isDefined(form.editableCondition)) {
      form.mon(
          form.editableCondition,
          {
            satisfied: function () {
              form.setEditable(true);
            },
            unsatisfied: function () {
              form.setEditable(false);
            },
            scope: form
          }
      );
    }
  },

  /**
   * Enable/Disable components marked with a "groupBind" property by checking that all fields marked with "bindGroup"
   * that matches, are valid.
   *
   * @private
   * @param {Ext.form.field.Base} field a field with a "bindGroup" property. "bindGroup" can be a space separated list of
   * groups
   */
  updateEnableState: function(field) {
    var form = field.up('form');

    if (Ext.isString(field['bindGroup'])) {
      Ext.Array.each(field['bindGroup'].split(' '), function(group) {
        var bindables = form.query('component[groupBind=' + group + ']'),
            validatables = form.query('field[bindGroup~=' + group + ']'),
            enabled;

        Ext.Array.each(bindables, function(bindable) {
          if (!Ext.isDefined(enabled)) {
            enabled = true;
            Ext.Array.each(validatables, function(validatable) {
              return enabled = validatable.isValid();
            });
          }
          if (enabled) {
            bindable.enable();
          }
          else {
            bindable.disable()
          }
        });
      });
    }
  }

});
