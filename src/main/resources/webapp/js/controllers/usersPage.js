define(['jquery', 'ractivejs', 'controllers/abstractPageController', 'rv!templates/usersPage',
	'i18n!locales', 'config', 'controllers/userItem', 'json', 'sort'], function($, Ractive,
	AbstractPageController, template, i18next, config, UserItem) {

    var UsersPageController = function(pageName) {
	AbstractPageController.call(this, pageName);

	this.items = new Array();
	this.autoRefreshCountDownTimerId = null;
	this.autoRefreshCountDownValue = null;
	this.lastAutoRefreshCountDownExecution = null;
    };

    UsersPageController.prototype = Object.create(AbstractPageController.prototype);
    UsersPageController.prototype.constructor = UsersPageController;

    UsersPageController.prototype.onLoad = function(mainContainer) {
	var controller = this;

	this.ractive = new Ractive({
	    el: mainContainer,
	    template: template
	});

	mainContainer.i18n();

	$.ajax({
	    url: "/proxy/user/list",
	    dataType: "json",
	    contentType: "application/json",
	    success: function(data) {
		// When users are retrieved, create the items
		data.forEach(function(user) {
		    controller.addUserInPage(user);
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
	this.getContainer().find('.autoRefreshCountDown').text(i18next.t('usersPage.autoRefresh', {
	    count: 1,
	    indefinite_article: true
	}));
	this.autoRefreshCountDownValue = config.autoRefreshDelay / 1000;
    };

    UsersPageController.prototype.onUnload = function() {
	// Clear all users.
	this.items.forEach(function(item) {
	    item.remove();
	});
	this.items.clear();
	this.stopAutoRefresh();

    };

    UsersPageController.prototype.addUserInPage = function(user) {
	var item = new UserItem(this.getContainer().find('.userItemContainer')), controller = this;
	item.setUser(user);
	this.items.push(item);

	// Initialize all buttons handlers

	// TODO
	// item.getEnableDisableButton().click(
	// function() {
	// controller.setPoolEnabled(pool.name, item
	// .getEnableDisableButton().text() == 'Enable');
	// });

    };

    UsersPageController.prototype.getUserItemFromName = function(userName) {
	return this.items.find(function(item) {
	    return item.user.name == userName;
	});
    };

    UsersPageController.prototype.refresh = function(onSuccess) {
	var controller = this;
	this.setIsRefreshing(true);

	// Reload user data
	$.ajax({
	    url: "/proxy/user/list",
	    dataType: "json",
	    contentType: "application/json",
	    success: function(data) {
		// When users are retrieved, update the userItems

		// Update existing users and create new ones.
		data.forEach(function(user) {
		    // Look for the userItem with the given name
		    var userItem = controller.items.find(function(item) {
			return item.user.name == user.name;
		    });

		    // If the user item does not exist, create it.
		    if (userItem == null) {
			controller.addUserInPage(user);
		    } else {
			// Else update the user and update the chart data.
			userItem.updateUser(user);
			userItem.reloadChartData(true);
		    }
		});

		// Then remove the users that does not more exist.
		controller.items.forEach(function(userItem) {
		    // Look for the user of the userItem in the received users.
		    var user = data.find(function(user) {
			return user.name == userItem.user.name;
		    });
		    // If the user is not in the received users, then delete it.
		    if (user == null) {
			userItem.remove();
			controller.items.removeItem(userItem);
		    }
		});

		// Once all users are present, sort them based on their names
		// and if they are active.
		controller.getContainer().find('.userItem').sort(function(a, b) {
		    var result = 0;
		    var aUser = $(a).data('user');
		    var bUser = $(b).data('user');
		    if (aUser.isActive && !bUser.isActive) {
			result = -1;
		    } else if (!aUser.isActive && bUser.isActive) {
			result = 1;
		    } else {
			if (aUser.name < $bUser.name) {
			    result = -1;
			} else if (aUser.name > bUser.name) {
			    result = 1;
			} else {
			    result = 0;
			}
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
		window.alert('Failed to get user list. Status: ' + textStatus + ', error: ' + errorThrown
			+ ', message: ' + jsonObject.message);
	    }
	});
    };

    /**
     * Change the appearance of the refresh button and start/stop the
     * auto-refresh.
     */
    UsersPageController.prototype.setIsRefreshing = function(isRefreshing) {
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
    UsersPageController.prototype.startAutoRefresh = function() {
	var controller = this, updateFunction;

	// Update the auto-refresh countdown
	var autoRefreshCountDown = this.getContainer().find('.autoRefreshCountDown');
	autoRefreshCountDown.text(i18next.t('usersPage.autoRefresh', {
	    count: controller.autoRefreshCountDownValue
	}));
	this.lastAutoRefreshCountDownExecution = Date.now();
	// Define the auto-refresh countdown update function
	updateFunction = function() {
	    var secondsSinceLastExecution = Math
		    .round((Date.now() - controller.lastAutoRefreshCountDownExecution) / 1000);
	    controller.lastAutoRefreshCountDownExecution = Date.now();
	    controller.autoRefreshCountDownValue -= secondsSinceLastExecution;

	    autoRefreshCountDown.text(i18next.t('usersPage.autoRefresh', {
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
    UsersPageController.prototype.resetAutoRefresh = function() {
	this.stopAutoRefresh();
	this.startAutoRefresh();
    };

    /**
     * Stop the auto-refresh
     */
    UsersPageController.prototype.stopAutoRefresh = function() {
	// Stop the auto-refresh countdown update timer.
	if (this.autoRefreshCountDownTimerId != null) {
	    window.clearInterval(this.autoRefreshCountDownTimerId);
	}

	// Update the auto-refresh countdown
	var autoRefreshCountDown = this.getContainer().find('.autoRefreshCountDown');
	i18next.t('autoRefresh');
	autoRefreshCountDown.text(i18next.t('usersPage.autoRefresh', {
	    count: 1,
	    indefinite_article: true
	}));
    };

    return UsersPageController;
});