define(['jquery', 'ractivejs', 'rv!templates/userItem', 'i18n!locales', 'config'], function($, Ractive,
	template, i18next, config) {

    var ConnectionItem = function(renderToElement) {
	var connectionItemId = ConnectionItem.nextConnectionItemId++;
	this.ractive = new Ractive({
	    el: renderToElement,
	    template: template,
	    data: {
		connectionItemId: connectionItemId
	    },
	    oncomplete: $.proxy(function() {
		this.connectionItemJquery.i18n();
	    }, this)
	});
	this.connectionItemJquery = $('#connectionItem-' + connectionItemId);
    };

    ConnectionItem.nextConnectionItemId = 0;

    ConnectionItem.prototype.setConnection = function(connection) {
	this.updateConnection(connection);
    };

    ConnectionItem.prototype.updateConnection = function(connection) {
	this.connection = connection;

	this.ractive.set(connection);
    }

});