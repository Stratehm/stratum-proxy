define(
	['jquery', 'ractivejs', 'controllers/abstractPageController', 'rv!templates/poolsPage', 'config', 'controllers/poolItem', 'controllers/addPoolPopup', 'json', 'sort'],
	function($, Ractive, AbstractPageController, template, config, PoolItem, AddPoolPopup) {

	    var PoolsPageController = function(pageName) {
		AbstractPageController.call(this, pageName);

		this.items = new Array();
		this.autoRefreshCountDownTimerId = null;
		this.autoRefreshCountDownValue = null;
		this.lastAutoRefreshCountDownExecution = null;
	    };

	    PoolsPageController.prototype = Object.create(AbstractPageController.prototype);
	    PoolsPageController.prototype.constructor = PoolsPageController;

	    PoolsPageController.prototype.onLoad = function(mainContainer) {
		var controller = this;

		var ractive = new Ractive({
		    el: mainContainer,
		    template: template
		});
		
		$.ajax({
		    url: "/proxy/pool/list",
		    dataType: "json",
		    contentType: "application/json",
		    success: function(data) {
			// When pools are retrieved, create the items
			data.forEach(function(pool) {
			    controller.addPoolInPage(pool);
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
		this.getContainer().find('.autoRefreshCountDown').text('Auto refresh in -- seconds.');
		this.autoRefreshCountDownValue = config.autoRefreshDelay / 1000;

		// Initialize the pool add button
		this.getContainer().find('.addPoolButton').off('click');
		this.getContainer().find('.addPoolButton').click(function() {
		    controller.openAddPool();
		});
	    };

	    PoolsPageController.prototype.onUnload = function() {
		// Clear all pools.
		this.items.forEach(function(item) {
		    item.remove();
		});
		this.items.clear();
		this.stopAutoRefresh();

	    };

	    PoolsPageController.prototype.addPoolInPage = function(pool) {
		var item = new PoolItem(this.getContainer().find('.poolItemContainer')), controller = this;
		item.setPool(pool);
		this.items.push(item);

		// Initialize all buttons handlers
		item.getSetHighestPriorityButton().click(function() {
		    controller.setPoolPriority(pool.name, 0);
		});

		item.getEnableDisableButton().click(function() {
		    controller.setPoolEnabled(pool.name, !item.pool.isEnabled);
		});

		item.getRemoveButton().click(function() {
		    controller.removePool(pool.name);
		});

		item.getEditButton().click(function() {
		    controller.openEditPool(pool.name);
		});
	    };
	    
	    PoolsPageController.prototype.getPoolItemFromName = function(poolName) {
		return this.items.find(function(item) {
		    return item.pool.name == poolName;
		});
	    };

	    PoolsPageController.prototype.refresh = function(onSuccess) {
		var controller = this;
		this.setIsRefreshing(true);

		// Reload pool data
		$.ajax({
		    url: "/proxy/pool/list",
		    dataType: "json",
		    contentType: "application/json",
		    success: function(data) {
			// When pools are retrieved, update the poolItems

			// Update existing pools and create new ones.
			data.forEach(function(pool) {
			    // Look for the poolItem with the given name
			    var poolItem = controller.items.find(function(item) {
				return item.pool.name == pool.name;
			    });

			    // If the pool item does not exist, create it.
			    if (poolItem == null) {
				controller.addPoolInPage(pool);
			    } else {
				// Else update the pool and update the chart
				// data.
				poolItem.updatePool(pool);
				poolItem.reloadChartData(true);
			    }
			});

			// Then remove the pools that does not more exist.
			controller.items.forEach(function(poolItem) {
			    // Look for the pool of the poolItem in the received
			    // pools.
			    var pool = data.find(function(pool) {
				return pool.name == poolItem.pool.name;
			    });
			    // If the pool is not in the received pools, then
			    // delete it.
			    if (pool == null) {
				poolItem.remove();
				controller.items.removeItem(poolItem);
			    }
			});

			// Once all pools are present, sort them based on their
			// priority
			controller.getContainer().find('.poolItem').sort(
				function(a, b) {
				    return $(a).data('priority') - $(b).data('priority');
				});

			controller.setIsRefreshing(false);

			if (onSuccess != undefined) {
			    onSuccess();
			}
		    },
		    error: function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to get pool list. Status: ' + textStatus + ', error: '
				+ errorThrown + ', message: ' + jsonObject.message);
		    }
		});
	    };

	    /**
	     * Set the priority of the pool with the given name
	     * 
	     * @param poolName
	     */
	    PoolsPageController.prototype.setPoolPriority = function(poolName, priority) {
		var controller = this;

		// Update the priority
		$.ajax({
		    url: "/proxy/pool/priority",
		    dataType: "json",
		    type: "POST",
		    data: JSON.stringify({
			poolName: poolName,
			priority: priority
		    }),
		    contentType: "application/json",
		    success: function(data) {
			// When priority is set, refresh the list.
			if (data.status != 'Done') {
			    window.alert('Failed to set the pool priority to 0. Message: ' + data.message);
			} else {
			    controller.refresh();
			}
		    },
		    error: function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to set the pool priority to 0. Status: ' + textStatus
				+ ', error: ' + errorThrown + ', message: ' + jsonObject.message);
		    }
		});
	    };

	    /**
	     * Open a pool edit box for the pool with the given name
	     * 
	     * @param poolName
	     */
	    PoolsPageController.prototype.openEditPool = function(poolName) {
		var controller = this, pool;

		pool = controller.getPoolItemFromName(poolName).pool;

		var modal = $('#editPoolModal').modal({
		    keyboard: true,
		    backdrop: 'static'
		}), controller = this;
		modal.find('.modal-title').text('Edit the pool ' + pool.name);

		// Initialize fields
		modal.find('#poolHostField').val(pool.host);
		modal.find('#usernameField').val(pool.username);
		modal.find('#passwordField').val(pool.password);
		modal.find('#priorityField').val(pool.priority);
		modal.find('#weightField').val(pool.weight);
		modal.find('#enableExtranonceSubscribeField').prop('checked',
			pool.isExtranonceSubscribeEnabled);
		modal.find('#appendWorkerNamesField').prop('checked', pool.appendWorkerNames);
		modal.find('#workerNameSeparatorField').val(pool.workerNamesSeparator);
		modal.find('#useWorkerPasswordField').prop('checked', pool.useWorkerPassword);

		modal
			.find('.validateButton')
			.off('click')
			.click(
				function() {

				    var poolHost = modal.find('#poolHostField').val(), username = modal.find(
					    '#usernameField').val(), password = modal.find('#passwordField')
					    .val(), priority = modal.find('#priorityField').val(), weight = modal
					    .find('#weightField').val(), enableExtranonceSubscribe = modal
					    .find('#enableExtranonceSubscribeField').is(':checked'), appendWorkerNames = modal
					    .find('#appendWorkerNamesField').is(':checked'), workerNameSeparator = modal
					    .find('#workerNameSeparatorField').val(), useWorkerPassword = modal
					    .find('#useWorkerPasswordField').is(':checked');

				    modal.modal('hide');
				    $.ajax({
					url: '/proxy/pool/update',
					dataType: "json",
					type: "POST",
					data: JSON.stringify({
					    poolName: pool.name,
					    poolHost: poolHost,
					    username: username,
					    password: password,
					    priority: priority,
					    weight: weight,
					    enableExtranonceSubscribe: enableExtranonceSubscribe,
					    appendWorkerNames: appendWorkerNames,
					    workerNameSeparator: workerNameSeparator,
					    useWorkerPassword: useWorkerPassword
					}),
					contentType: "application/json",
					success: function(data) {
					    // When priority is set, refresh the
					    // list.
					    if (data.status == 'Failed') {
						window.alert('Failed to update the pool. Message: '
							+ data.message);
					    } else if (data.status == 'PartiallyDone') {
						window.alert('Pool updated but not started. Message: '
							+ data.message);
						controller.refresh();
					    } else {
						controller.refresh();
					    }
					},
					error: function(request, textStatus, errorThrown) {
					    var jsonObject = JSON.parse(request.responseText);
					    window.alert('Failed to update the pool. Status: ' + textStatus
						    + ', error: ' + errorThrown + ', message: '
						    + jsonObject.message);
					}
				    });
				});

	    };

	    /**
	     * Set the pool with the given name enabled/disabled
	     * 
	     * @param poolName
	     */
	    PoolsPageController.prototype.setPoolEnabled = function(poolName, isEnabled) {
		var controller = this, url;

		if (isEnabled) {
		    url = "/proxy/pool/enable";
		} else {
		    url = "/proxy/pool/disable";
		}

		// Reload pool data
		$.ajax({
		    url: url,
		    dataType: "json",
		    type: "POST",
		    data: JSON.stringify({
			poolName: poolName,
		    }),
		    contentType: "application/json",
		    success: function(data) {
			// When priority is set, refresh the list.
			if (data.status != 'Done') {
			    window.alert('Failed to change the state of the pool. Message: ' + data.message);
			} else {
			    controller.refresh();
			}
		    },
		    error: function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to change the state of the pool. Status: ' + textStatus
				+ ', error: ' + errorThrown + ', message: ' + jsonObject.message);
		    }
		});
	    };

	    /**
	     * Remove the pool with the given name. Ask a confirmation.
	     */
	    PoolsPageController.prototype.removePool = function(poolName) {
		var modal = $('#poolRemoveConfirmationModal').modal({
		    keyboard: true,
		    backdrop: true
		}), controller = this, removePoolFunction;
		modal.find('.modal-title').text('Keep history confirmation');
		modal.find('.modal-body').text(
			'Do you want to keep the hash rate history for ' + poolName + ' ?');

		removePoolFunction = function(keepHistory) {
		    modal.modal('hide');
		    $.ajax({
			url: '/proxy/pool/remove',
			dataType: "json",
			type: "POST",
			data: JSON.stringify({
			    poolName: poolName,
			    keepHistory: keepHistory
			}),
			contentType: "application/json",
			success: function(data) {
			    // When priority is set, refresh the list.
			    if (data.status != 'Done') {
				window.alert('Failed to remove the pool. Message: ' + data.message);
			    } else {
				controller.refresh();
			    }
			},
			error: function(request, textStatus, errorThrown) {
			    var jsonObject = JSON.parse(request.responseText);
			    window.alert('Failed remove the pool. Status: ' + textStatus + ', error: '
				    + errorThrown + ', message: ' + jsonObject.message);
			}
		    });
		};

		modal.find('.yesButton').off('click').click(function() {
		    removePoolFunction(true);
		});

		modal.find('.noButton').off('click').click(function() {
		    removePoolFunction(false);
		});
	    };

	    /**
	     * Change the appearance of the refresh button and start/stop the
	     * auto-refresh.
	     */
	    PoolsPageController.prototype.setIsRefreshing = function(isRefreshing) {
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
	    PoolsPageController.prototype.startAutoRefresh = function() {
		var controller = this, updateFunction;

		// Update the auto-refresh countdown
		var autoRefreshCountDown = this.getContainer().find('.autoRefreshCountDown');
		autoRefreshCountDown.text('Auto refresh in ' + controller.autoRefreshCountDownValue
			+ ' seconds.');
		this.lastAutoRefreshCountDownExecution = Date.now();
		// Define the auto-refresh countdown update function
		updateFunction = function() {
		    var secondsSinceLastExecution = Math
			    .round((Date.now() - controller.lastAutoRefreshCountDownExecution) / 1000);
		    controller.lastAutoRefreshCountDownExecution = Date.now();
		    controller.autoRefreshCountDownValue -= secondsSinceLastExecution;

		    autoRefreshCountDown.text('Auto refresh in ' + controller.autoRefreshCountDownValue
			    + ' seconds.');

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
	    PoolsPageController.prototype.resetAutoRefresh = function() {
		this.stopAutoRefresh();
		this.startAutoRefresh();
	    };

	    /**
	     * Stop the auto-refresh
	     */
	    PoolsPageController.prototype.stopAutoRefresh = function() {
		// Stop the auto-refresh countdown update timer.
		if (this.autoRefreshCountDownTimerId != null) {
		    window.clearInterval(this.autoRefreshCountDownTimerId);
		}

		// Update the auto-refresh countdown
		var autoRefreshCountDown = this.getContainer().find('.autoRefreshCountDown');
		autoRefreshCountDown.text('Auto refresh in -- seconds.');
	    };

	    /**
	     * Open a pool add popup.
	     */
	    PoolsPageController.prototype.openAddPool = function() {
		new AddPoolPopup(this);
	    };

	    return PoolsPageController;
	});
