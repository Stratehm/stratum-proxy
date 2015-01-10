define(['jquery', 'ractivejs', 'controllers/abstractPageController', 'rv!templates/settingsPage',
	'i18n!locales', 'config'], function($, Ractive, AbstractPageController, template, i18next, config) {

    var SettingsPageController = function(pageName) {
	AbstractPageController.call(this, pageName);
    };

    SettingsPageController.prototype = Object.create(AbstractPageController.prototype);
    SettingsPageController.prototype.constructor = SettingsPageController;

    SettingsPageController.prototype.onLoad = function(mainContainer) {
	var controller = this;

	this.ractive = new Ractive({
	    el: mainContainer,
	    template: template,
	    oncomplete: $.proxy(function() {
		mainContainer.i18n();
	    }, this)
	});
    };

    return SettingsPageController;
});