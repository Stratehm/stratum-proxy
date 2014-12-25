define(['jquery', 'ractivejs', 'controllers/abstractPageController', 'rv!templates/connectionsPage',
	'i18n!locales', 'config'], function($, Ractive, AbstractPageController, template, i18next, config) {

    var ConnectionsPageController = function(pageName) {
	AbstractPageController.call(this, pageName);
    };

    ConnectionsPageController.prototype = Object.create(AbstractPageController.prototype);
    ConnectionsPageController.prototype.constructor = ConnectionsPageController;

    ConnectionsPageController.prototype.onLoad = function(mainContainer) {
	var controller = this;

	this.ractive = new Ractive({
	    el: mainContainer,
	    template: template
	});

	mainContainer.i18n();
    };

    return ConnectionsPageController;
});