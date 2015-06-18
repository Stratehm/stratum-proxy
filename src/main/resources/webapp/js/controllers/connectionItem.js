define(['jquery', 'ractivejs', 'rv!templates/connectionItem', 'i18n!locales', 'config','managers/authenticationManager'], function($, Ractive,
	template, i18next, config, authenticationManager) {

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
	
	this.updateAccessibleItems();
	document.addEventListener('loginSuccess', function() {
	    self.updateAccessibleItems();
	}, false);
    };

    ConnectionItem.nextConnectionItemId = 0;

    ConnectionItem.prototype.setConnection = function(connection) {
	this.updateConnection(connection);
    };

    ConnectionItem.prototype.updateConnection = function(connection) {
	this.connection = connection;

	this.ractive.set(connection);
	
	this.connectionItemJquery.data('connection', connection);
    };
    
    ConnectionItem.prototype.remove = function() {
	this.connectionItemJquery.remove();
    };
    
    ConnectionItem.prototype.getKickButton = function() {
	return this.connectionItemJquery.find(".kickConnection");
    };

    ConnectionItem.prototype.getBanIpButton = function() {
	return this.connectionItemJquery.find(".banIp");
    };
    
    ConnectionItem.prototype.getKickIpButton = function() {
	return this.connectionItemJquery.find(".kickIp");
    };
    
    /**
     * Update the displayed items based on the authorization.
     */
    ConnectionItem.prototype.updateAccessibleItems = function() {
	if(!authenticationManager.isAuthenticated) {
	    this.connectionItemJquery.find('.kickConnection, .kickIp, .banIp').hide();
	} else {
	    this.connectionItemJquery.find('.kickConnection, .kickIp, .banIp').show();
	}
    };

    return ConnectionItem;
});