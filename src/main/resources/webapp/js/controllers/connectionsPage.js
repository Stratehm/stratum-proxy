define(['jquery', 'ractivejs', 'controllers/abstractPageController', 'rv!templates/connectionsPage',
		'i18n!locales', 'config', 'controllers/connectionItem', 'controllers/confirmationPopup', 'json',
		'sort'],
	function($, Ractive, AbstractPageController, template, i18next, config, ConnectionItem, ConfirmationPopup) {

	    var ConnectionsPageController = function(pageName) {
		AbstractPageController.call(this, pageName);
		
		this.items = new Array();
		this.autoRefreshCountDownTimerId = null;
		this.autoRefreshCountDownValue = null;
		this.lastAutoRefreshCountDownExecution = null;
	    };

	    ConnectionsPageController.prototype = Object.create(AbstractPageController.prototype);
	    ConnectionsPageController.prototype.constructor = ConnectionsPageController;

	    ConnectionsPageController.prototype.onLoad = function(mainContainer) {
		var controller = this;

		this.ractive = new Ractive({
		    el: mainContainer,
		    template: template,
		    oncomplete: $.proxy(function() {
			mainContainer.i18n();
		    }, this)
		});

		$.ajax({
		    url: "proxy/connection/list",
		    dataType: "json",
		    contentType: "application/json",
		    success: function(data) {
			// When connections are retrieved, create the items
			data.forEach(function(connection) {
			    controller.addConnectionInPage(connection);
			});

			controller.startAutoRefresh();
		    }
		});

		// Add the click event on the refresh button
		this.getContainer().find('.refreshButton').off('click');
		this.getContainer().find('.refreshButton').click(function() {
		    controller.refresh();
		});

		// Initialize the auto-refresh countdown
		this.getContainer().find('.autoRefreshCountDown').text(
			i18next.t('connectionsPage.autoRefresh', {
			    count: 1,
			    indefinite_article: true
			}));
		this.autoRefreshCountDownValue = config.autoRefreshDelay / 1000;
	    };

	    ConnectionsPageController.prototype.onUnload = function() {
		// Clear all connections.
		this.items.forEach(function(item) {
		    item.remove();
		});
		this.items.clear();
		this.stopAutoRefresh();

	    };

	    ConnectionsPageController.prototype.addConnectionInPage = function(connection) {
		var item = new ConnectionItem(this.getContainer().find('.connectionsItemContainer')), controller = this;
		item.setConnection(connection);
		this.items.push(item);
		
		// Initialize all buttons handlers
		item.getKickButton().click(function() {
		    controller.kickConnection(connection);
		});

		item.getBanIpButton().click(function() {
		    controller.banIp(connection);
		});
		
		item.getKickIpButton().click(function() {
		    controller.kickIp(connection);
		});

	    };
	    
	    ConnectionsPageController.prototype.kickConnection = function(connection) {
		var controller = this;

		$.ajax({
		    url: 'proxy/connection/kick',
		    dataType: "json",
		    type: "POST",
		    data: JSON.stringify({
			address: connection.remoteHost,
			port: connection.remotePort
		    }),
		    contentType: "application/json",
		    success: function(data) {
			if (data.status != 'Done') {
			    window.alert('Failed to kick the connection. Message: ' + data.message);
			} else {
			    controller.refresh();
			}
		    },
		    error: function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to kick the connection. Status: ' + textStatus + ', error: ' + errorThrown
				+ ', message: ' + jsonObject.message);
		    }
		});
	    }
	    
	    ConnectionsPageController.prototype.banIp = function(connection) {
		var controller = this;

		var banIpFunction = function() {
		    $.ajax({
			url: 'proxy/address/ban',
			dataType: "json",
			type: "POST",
			data: JSON.stringify({
			    address: connection.remoteHost
			}),
			contentType: "application/json",
			success: function(data) {
			    if (data.status != 'Done') {
				window.alert('Failed to ban the IP. Message: ' + data.message);
			    } else {
				controller.refresh();
			    }
			},
			error: function(request, textStatus, errorThrown) {
			    var jsonObject = JSON.parse(request.responseText);
			    window.alert('Failed to ban the IP. Status: ' + textStatus + ', error: ' + errorThrown
				    + ', message: ' + jsonObject.message);
			}
		    });
		};

		var modal = new ConfirmationPopup({
		    title: i18next.t('connectionsPage.banIp.confirmationPopup.title'),
		    message: i18next.t('connectionsPage.banIp.confirmationPopup.message', {
			ip: connection.remoteHost
		    }),
		    yesCallback: banIpFunction,
		    displayCancelButton: false
		});
	    }
	    
	    ConnectionsPageController.prototype.kickIp = function(connection) {
		var controller = this;

		var kickIpFunction = function() {
		    $.ajax({
			url: 'proxy/address/kick',
			dataType: "json",
			type: "POST",
			data: JSON.stringify({
			    address: connection.remoteHost
			}),
			contentType: "application/json",
			success: function(data) {
			    if (data.status != 'Done') {
				window.alert('Failed to kick the IP. Message: ' + data.message);
			    } else {
				controller.refresh();
			    }
			},
			error: function(request, textStatus, errorThrown) {
			    var jsonObject = JSON.parse(request.responseText);
			    window.alert('Failed to kick the IP. Status: ' + textStatus + ', error: ' + errorThrown
				    + ', message: ' + jsonObject.message);
			}
		    });
		};

		var modal = new ConfirmationPopup({
		    title: i18next.t('connectionsPage.kickIp.confirmationPopup.title'),
		    message: i18next.t('connectionsPage.kickIp.confirmationPopup.message', {
			ip: connection.remoteHost
		    }),
		    yesCallback: kickIpFunction,
		    displayCancelButton: false
		});
	    }

	    ConnectionsPageController.prototype.refresh = function(onSuccess) {
		var controller = this;
		this.setIsRefreshing(true);

		// Reload connection data
		$.ajax({
		    url: "proxy/connection/list",
		    dataType: "json",
		    contentType: "application/json",
		    success: function(data) {
			// When connections are retrieved, update the
			// connectionItems

			// Update existing connections and create new ones.
			data.forEach(function(connection) {
			    // Look for the connectionItem with the given name
			    var connectionItem = controller.items.find(function(item) {
				return item.connection.remoteHost == connection.remoteHost && item.connection.remotePort == connection.remotePort;
			    });

			    // If the connection item does not exist, create it.
			    if (connectionItem == null) {
				controller.addConnectionInPage(connection);
			    } else {
				// Else update the connection and update the
				// chart data.
				connectionItem.updateConnection(connection);
			    }
			});

			// Then remove the connections that does not more exist.
			controller.items.forEach(function(connectionItem) {
			    // Look for the connection of the connectionItem in
			    // the received connections.
			    var connection = data.find(function(connection) {
				return connection.remoteHost == connectionItem.connection.remoteHost && connection.remotePort == connectionItem.connection.remotePort;
			    });
			    // If the connection is not in the received
			    // connections, then delete it.
			    if (connection == null) {
				connectionItem.remove();
				controller.items.removeItem(connectionItem);
			    }
			});

			// Once all connections are present, sort them based on
			// their remoteHost
			controller.getContainer().find('.connectionItem').sort(function(a, b) {
			    var result = 0;
			    var aConnection = $(a).data('connection');
			    var bConnection = $(b).data('connection');
			    if (aConnection.remoteHost < $bConnection.remoteHost) {
				result = -1;
			    } else if (aConnection.remoteHost > bConnection.remoteHost) {
				result = 1;
			    } else {
				result = 0;
			    }

			    return result;
			});

			controller.setIsRefreshing(false);

			if (onSuccess != undefined) {
			    onSuccess();
			}
		    },
		    error: function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to get connection list. Status: ' + textStatus + ', error: '
				+ errorThrown + ', message: ' + jsonObject.message);
		    }
		});
	    };

	    /**
	     * Change the appearance of the refresh button and start/stop the
	     * auto-refresh.
	     */
	    ConnectionsPageController.prototype.setIsRefreshing = function(isRefreshing) {
		var refreshButton = this.getContainer().find('.refreshButton');
		if (isRefreshing) {
		    this.stopAutoRefresh();
		    refreshButton.attr('disabled', 'true');
		    refreshButton.find('i').addClass('spin');
		} else {
		    this.startAutoRefresh();
		    refreshButton.removeAttr('disabled');
		    refreshButton.find('i').removeClass('spin');
		}
	    };

	    /**
	     * Start the auto-refresh
	     */
	    ConnectionsPageController.prototype.startAutoRefresh = function() {
		var controller = this, updateFunction;

		// Update the auto-refresh countdown
		var autoRefreshCountDown = this.getContainer().find('.autoRefreshCountDown');
		autoRefreshCountDown.text(i18next.t('connectionsPage.autoRefresh', {
		    count: controller.autoRefreshCountDownValue
		}));
		this.lastAutoRefreshCountDownExecution = Date.now();
		// Define the auto-refresh countdown update function
		updateFunction = function() {
		    var secondsSinceLastExecution = Math
			    .round((Date.now() - controller.lastAutoRefreshCountDownExecution) / 1000);
		    controller.lastAutoRefreshCountDownExecution = Date.now();
		    controller.autoRefreshCountDownValue -= secondsSinceLastExecution;

		    autoRefreshCountDown.text(i18next.t('connectionsPage.autoRefresh', {
			count: controller.autoRefreshCountDownValue
		    }));

		    if (controller.autoRefreshCountDownValue <= 0) {
			controller.refresh();
			controller.startAutoRefresh();
			controller.autoRefreshCountDownValue = config.autoRefreshDelay / 1000;
		    }
		};
		// Start the auto-refresh countdown update timer.
		this.autoRefreshCountDownTimerId = window.setInterval(updateFunction, 1000);

	    };

	    /**
	     * Reset the delay before next auto-refresh
	     */
	    ConnectionsPageController.prototype.resetAutoRefresh = function() {
		this.stopAutoRefresh();
		this.startAutoRefresh();
	    };

	    /**
	     * Stop the auto-refresh
	     */
	    ConnectionsPageController.prototype.stopAutoRefresh = function() {
		// Stop the auto-refresh countdown update timer.
		if (this.autoRefreshCountDownTimerId != null) {
		    window.clearInterval(this.autoRefreshCountDownTimerId);
		}

		// Update the auto-refresh countdown
		var autoRefreshCountDown = this.getContainer().find('.autoRefreshCountDown');
		i18next.t('autoRefresh');
		autoRefreshCountDown.text(i18next.t('connectionsPage.autoRefresh', {
		    count: 1,
		    indefinite_article: true
		}));
	    };

	    return ConnectionsPageController;
});