/*
 * Define the page controller class
 */
var PageController = function(pageName) {
	this.pageName = pageName;
	this.containerJquery = $('#' + pageName);
	this.containerJquery.hide();
};

PageController.prototype.hide = function() {
	this.containerJquery.hide();
};

PageController.prototype.show = function() {
	this.containerJquery.show();
};

PageController.prototype.load = function() {
	this.show();
	if (this.onLoad != undefined) {
		this.onLoad();
	}
};

PageController.prototype.unload = function() {
	this.hide();
	if (this.onUnload != undefined) {
		this.onUnload();
	}
};

PageController.prototype.refresh = function() {
	// Do nothing. Will be implemented by sub-classes
};

/*
 * Define the Pools page controller
 */
var PoolsPageController = function(pageName) {
	PageController.call(this, pageName);
};

PoolsPageController.prototype = Object.create(PageController.prototype);
PoolsPageController.prototype.constructor = PageController;

PoolsPageController.prototype.onLoad = function() {
	var controller = this;
	$.ajax({
		url : "/proxy/pool/list",
		dataType : "json",
		contentType : "application/json",
		success : function(data) {
			// When pools are retrieved, create the items
			data.forEach(function(pool) {
				controller.addPoolInPage(pool);
			});

			controller.startAutoRefresh();
		}
	});

	// Add the click event on the refresh button
	this.containerJquery.find('.refreshButton').off('click');
	this.containerJquery.find('.refreshButton').click(function() {
		controller.refresh();
	});

	// Initialize the auto-refresh countdown
	this.containerJquery.find('.autoRefreshCountDown').text(
			'Auto refresh in -- seconds.');
	this.autoRefreshCountDownValue = autoRefreshDelay / 1000;

	// Initialize the pool add button
	this.containerJquery.find('.addPoolButton').off('click');
	this.containerJquery.find('.addPoolButton').click(function() {
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

PoolsPageController.prototype.items = new Array();
PoolsPageController.prototype.addPoolInPage = function(pool) {
	var item = new PoolItem(), controller = this;
	item.setPool(pool);
	this.items.push(item);
	this.containerJquery.find('.poolItemContainer').append(item.poolItemJquery);

	// Initialize all buttons handlers
	item.getSetHighestPriorityButton().click(function() {
		controller.setPoolPriority(pool.name, 0);
	});

	item.getEnableDisableButton().click(
			function() {
				controller.setPoolEnabled(pool.name, item
						.getEnableDisableButton().text() == 'Enable');
			});

	item.getRemoveButton().click(function() {
		controller.removePool(pool.name);
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
		url : "/proxy/pool/list",
		dataType : "json",
		contentType : "application/json",
		success : function(data) {
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
					// Else update the pool and update the chart data.
					poolItem.updatePool(pool);
					poolItem.reloadChartData(true);
				}
			});

			// Then remove the pools that does not more exist.
			controller.items.forEach(function(poolItem) {
				// Look for the pool of the poolItem in the received pools.
				var pool = data.find(function(pool) {
					return pool.name == poolItem.pool.name;
				});
				// If the pool is not in the received pools, then delete it.
				if (pool == null) {
					poolItem.remove();
					controller.items.removeItem(poolItem);
				}
			});

			// Once all pools are present, sort them based on their priority
			controller.containerJquery.find('.poolItemContainer > .poolItem')
					.sort(function(a, b) {
						return $(a).data('priority') - $(b).data('priority');
					});

			controller.setIsRefreshing(false);

			if (onSuccess != undefined) {
				onSuccess();
			}
		},
		error : function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to get pool list. Status: ' + textStatus
					+ ', error: ' + errorThrown + ', message: '
					+ jsonObject.message);
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

	// Reload pool data
	$.ajax({
		url : "/proxy/pool/priority",
		dataType : "json",
		type : "POST",
		data : JSON.stringify({
			poolName : poolName,
			priority : priority
		}),
		contentType : "application/json",
		success : function(data) {
			// When priority is set, refresh the list.
			if (data.status != 'Done') {
				window.alert('Failed to set the pool priority to 0. Message: '
						+ data.message);
			} else {
				controller.refresh();
			}
		},
		error : function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to set the pool priority to 0. Status: '
					+ textStatus + ', error: ' + errorThrown + ', message: '
					+ jsonObject.message);
		}
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
	$
			.ajax({
				url : url,
				dataType : "json",
				type : "POST",
				data : JSON.stringify({
					poolName : poolName,
				}),
				contentType : "application/json",
				success : function(data) {
					// When priority is set, refresh the list.
					if (data.status != 'Done') {
						window
								.alert('Failed to change the state of the pool. Message: '
										+ data.message);
					} else {
						controller.refresh();
					}
				},
				error : function(request, textStatus, errorThrown) {
					var jsonObject = JSON.parse(request.responseText);
					window
							.alert('Failed to change the state of the pool. Status: '
									+ textStatus
									+ ', error: '
									+ errorThrown
									+ ', message: ' + jsonObject.message);
				}
			});
};

/**
 * Remove the pool with the given name. Ask a confirmation.
 */
PoolsPageController.prototype.removePool = function(poolName) {
	var modal = $('#confirmationModal').modal({
		keyboard : true,
		backdrop : true
	}), controller = this;
	modal.find('.modal-title').text('Confirmation');
	modal.find('.modal-body').text(
			'Do you really want to remove the pool ' + poolName + ' ?');
	modal.find('.yesButton').off('click').click(
			function() {
				modal.modal('hide');
				$.ajax({
					url : '/proxy/pool/remove',
					dataType : "json",
					type : "POST",
					data : JSON.stringify({
						poolName : poolName,
					}),
					contentType : "application/json",
					success : function(data) {
						// When priority is set, refresh the list.
						if (data.status != 'Done') {
							window.alert('Failed to remove the pool. Message: '
									+ data.message);
						} else {
							controller.refresh();
						}
					},
					error : function(request, textStatus, errorThrown) {
						var jsonObject = JSON.parse(request.responseText);
						window.alert('Failed remove the pool. Status: '
								+ textStatus + ', error: ' + errorThrown
								+ ', message: ' + jsonObject.message);
					}
				});
			});
};

/**
 * Change the appearance of the refresh button and start/stop the auto-refresh.
 */
PoolsPageController.prototype.setIsRefreshing = function(isRefreshing) {
	var refreshButton = this.containerJquery.find('.refreshButton');
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
 * Properties to manage auto-refresh
 */
PoolsPageController.prototype.autoRefreshCountDownTimerId = null;
PoolsPageController.prototype.autoRefreshCountDownValue = null;
PoolsPageController.prototype.lastAutoRefreshCountDownExecution = null;
/**
 * Start the auto-refresh
 */
PoolsPageController.prototype.startAutoRefresh = function() {
	var controller = this, updateFunction;

	// Update the auto-refresh countdown
	var autoRefreshCountDown = this.containerJquery
			.find('.autoRefreshCountDown');
	autoRefreshCountDown.text('Auto refresh in '
			+ controller.autoRefreshCountDownValue + ' seconds.');
	this.lastAutoRefreshCountDownExecution = Date.now();
	// Define the auto-refresh countdown update function
	updateFunction = function() {
		var secondsSinceLastExecution = Math
				.round((Date.now() - controller.lastAutoRefreshCountDownExecution) / 1000);
		controller.lastAutoRefreshCountDownExecution = Date.now();
		controller.autoRefreshCountDownValue -= secondsSinceLastExecution;

		autoRefreshCountDown.text('Auto refresh in '
				+ controller.autoRefreshCountDownValue + ' seconds.');

		if (controller.autoRefreshCountDownValue <= 0) {
			controller.refresh();
			controller.startAutoRefresh();
			controller.autoRefreshCountDownValue = autoRefreshDelay / 1000;
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
	var autoRefreshCountDown = this.containerJquery
			.find('.autoRefreshCountDown');
	autoRefreshCountDown.text('Auto refresh in -- seconds.');
};

/**
 * Open a pool add popup.
 */
PoolsPageController.prototype.openAddPool = function() {
	var modal = $('#addPoolModal').modal({
		keyboard : true,
		backdrop : 'static'
	}), controller = this;
	modal.find('.modal-title').text('Add a pool');
	var modal = $('#addPoolModal');
	modal
			.find('.validateButton')
			.off('click')
			.click(
					function() {

						var poolName = modal.find('#poolNameField').val(), poolHost = modal
								.find('#poolHostField').val(), username = modal
								.find('#usernameField').val(), password = modal
								.find('#passwordField').val(), priority = modal
								.find('#priorityField').val(), enableExtranonceSubscribe = modal
								.find('#enableExtranonceSubscribeField').is(
										':checked'), isEnabled = modal.find(
								'#isEnabledField').is(':checked'), appendWorkerNames = modal
								.find('#appendWorkerNamesField').is(':checked'), workerNameSeparator = modal
								.find('#workerNameSeparatorField').val(), useWorkerPassword = modal
								.find('#useWorkerPasswordField').is(':checked');

						modal.modal('hide');
						$
								.ajax({
									url : '/proxy/pool/add',
									dataType : "json",
									type : "POST",
									data : JSON
											.stringify({
												poolName : poolName,
												poolHost : poolHost,
												username : username,
												password : password,
												priority : priority,
												enableExtranonceSubscribe : enableExtranonceSubscribe,
												isEnabled : isEnabled,
												appendWorkerNames : appendWorkerNames,
												workerNameSeparator : workerNameSeparator,
												useWorkerPassword : useWorkerPassword
											}),
									contentType : "application/json",
									success : function(data) {
										// When priority is set, refresh the
										// list.
										if (data.status == 'Failed') {
											window
													.alert('Failed to add the pool. Message: '
															+ data.message);
										} else if (data.status == 'PartiallyDone') {
											window
													.alert('Pool added but not started. Message: '
															+ data.message);
											controller.refresh();
										} else {
											controller.refresh();
										}
									},
									error : function(request, textStatus,
											errorThrown) {
										var jsonObject = JSON
												.parse(request.responseText);
										window
												.alert('Failed to add the pool. Status: '
														+ textStatus
														+ ', error: '
														+ errorThrown
														+ ', message: '
														+ jsonObject.message);
									}
								});
					});
};

/*
 * Controller of the logs page.
 */
var LogsPageController = function(pageName) {
	PageController.call(this, pageName);
};

LogsPageController.prototype = Object.create(PageController.prototype);
LogsPageController.prototype.constructor = PageController;

/**
 * Load the logs page
 */
LogsPageController.prototype.onLoad = function() {
	this.loadLevel();
	this.refresh();
	this.startAutoRefresh();
};

/**
 * Unload the logs page
 */
LogsPageController.prototype.onUnload = function() {
	this.stopAutoRefresh();
	this.clear();
};

/**
 * Load and select the log level.
 */
LogsPageController.prototype.loadLevel = function() {
	var selectPicker = this.containerJquery.find('.logLevelSelectPicker'), controller = this;
	selectPicker.prop('disabled', true);
	selectPicker.selectpicker('refresh');

	$.ajax({
		url : '/proxy/log/level',
		dataType : "json",
		type : "GET",
		contentType : "application/json",
		success : function(data) {
			if (data != undefined) {
				selectPicker.selectpicker('val', data.logLevel);
				selectPicker.prop('disabled', false);
				selectPicker.selectpicker('refresh');
				controller.initLogLevelSelect();
			}
		},
		error : function(request, textStatus, errorThrown) {
			window.alert('Failed to get the log level. Status: ' + textStatus
					+ ', error: ' + errorThrown);
		}
	});
};

/**
 * Initialize the select input to change the log level
 */
LogsPageController.prototype.initLogLevelSelect = function() {
	var selectPicker = this.containerJquery.find('.logLevelSelectPicker');
	selectPicker.off('change');
	selectPicker.change(function() {
		var selectedValue = selectPicker.find('option:selected').text();
		selectPicker.selectpicker('refresh');
		$.ajax({
			url : '/proxy/log/level',
			dataType : "json",
			type : "POST",
			data : JSON.stringify({
				logLevel : selectedValue
			}),
			contentType : "application/json",
			error : function(request, textStatus, errorThrown) {
				window.alert('Failed to change the log level. Status: '
						+ textStatus + ', error: ' + errorThrown);
			}
		});
	});
};

/**
 * Refresh the logs.
 */
LogsPageController.prototype.refresh = function() {
	var controller = this;
	$.ajax({
		url : '/proxy/log/since',
		dataType : "json",
		type : "POST",
		data : JSON.stringify({
			timestamp : controller.lastLogLineTimestamp
		}),
		contentType : "application/json",
		success : function(data) {
			if (data != undefined) {
				controller.appendLogs(data);
			}
		},
		error : function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to get the last log lines. Status: '
					+ textStatus + ', error: ' + errorThrown + ', message: '
					+ jsonObject.message);
		}
	});
};

/**
 * Clear the logs panel
 */
LogsPageController.prototype.clear = function() {
	this.containerJquery.find('.logsContainer').text("");
	this.lastLogLineTimestamp = null;
};

/**
 * Append the given logs data.
 */
LogsPageController.prototype.appendLogs = function(logsData) {
	var logString = "", controller = this, isScrolledBottom = false, scrollContainer = $('html');

	// Check if the scroll is already in bottom. If so, keep the scroll at the
	// bottom after log lines append.
	isScrolledBottom = scrollContainer[0].scrollHeight
			- scrollContainer[0].scrollTop === scrollContainer[0].clientHeight;

	logsData.forEach(function(logEntry) {
		controller.lastLogLineTimestamp = logEntry.key;
		logString += logEntry.value;
	});

	this.containerJquery.find('.logsContainer').append(logString);

	// If the page was at the bottom position, keep it in this position. Else do
	// nothing.
	if (isScrolledBottom) {
		scrollContainer.scrollTop(scrollContainer[0].scrollHeight);
	}
};

/**
 * Used by auto-refresh
 */
LogsPageController.prototype.autoRefreshCountDownTimerId = null;
LogsPageController.prototype.lastLogLineTimestamp = null;

/**
 * Start the logs auto-refresh process.
 */
LogsPageController.prototype.startAutoRefresh = function() {
	var controller = this;
	// Start the auto-refresh countdown update timer.
	this.autoRefreshCountDownTimerId = window.setInterval(function() {
		controller.refresh();
	}, 1000);
};

/**
 * Stop the log auto-refresh
 */
LogsPageController.prototype.stopAutoRefresh = function() {
	// Stop the logs auto-refresh
	if (this.autoRefreshCountDownTimerId != null) {
		window.clearInterval(this.autoRefreshCountDownTimerId);
	}
};

/*
 * Define a pool item linked to a view
 */
var PoolItem = function() {
	this.poolItemJquery = createPoolItem();
};

PoolItem.prototype.setPool = function(pool) {
	// Update the values in the panel
	this.updatePool(pool);

	// Initialize a tooltip when the text overflows
	$('.tooltipOnOverflow').bind('mouseenter', function() {
		var $this = $(this);
		if (this.offsetWidth < this.scrollWidth && !$this.attr('title')) {
			$this.attr('title', $this.text());
		}
	});

	// Reload the data of the chart
	this.reloadChartData(false);
};

PoolItem.prototype.initChart = function() {
	// Remove the loader
	this.poolItemJquery.find(".hashrateChart").empty();

	// Create the chart
	this.poolItemJquery.find(".hashrateChart").highcharts(
			'StockChart',
			{
				chart : {
					zoomType : 'x'
				},
				rangeSelector : {
					enabled : true,
					inputEnabled : false,
					buttons : [ {
						type : 'hour',
						count : 1,
						text : '1h'
					}, {
						type : 'day',
						count : 1,
						text : '1d'
					}, {
						type : 'week',
						count : 1,
						text : '1w'
					}, {
						type : 'day',
						count : 7,
						text : '1m'
					}, {
						type : 'month',
						count : 1,
						text : '1y'
					}, {
						type : 'all',
						text : 'All'
					} ]
				},
				navigator : {
					enabled : false
				},
				scrollbar : {
					enabled : false
				},
				title : {
					text : 'Hashrate'
				},
				xAxis : {
					type : 'datetime',
					title : {
						text : 'Date'
					},
					ordinal : false
				},
				yAxis : [ {
					min : 0,
					labels : {
						align : 'left'
					}
				}, {
					linkedTo : 0,
					opposite : false,
					title : {
						text : 'Hashrate'
					}
				} ],
				tooltip : {
					shared : true,
					positioner : function(labelWidth, labelHeight, point) {
						var xPos = point.plotX + this.chart.plotLeft + 10;
						var yPos = point.plotY + this.chart.plotTop;

						if (point.plotX + labelWidth > this.chart.chartWidth
								- this.chart.plotLeft) {
							xPos = point.plotX + this.chart.plotLeft
									- labelWidth - 10;
						}

						return {
							x : xPos,
							y : yPos
						};
					}
				},
				plotOptions : {
					area : {
						stacking : 'normal',
						lineWidth : 1,
						marker : {
							enabled : false
						}
					}
				},
				series : [ {
					name : 'Accepted',
					type : 'area'
				}, {
					name : 'Rejected',
					type : 'area'
				} ]
			});

	// Associate the chart with the poolItem
	this.chart = this.poolItemJquery.find(".hashrateChart").highcharts();
};

PoolItem.prototype.updatePool = function(pool) {
	this.pool = pool;
	this.poolItemJquery.find('.panel-title').text(pool.name);

	this.poolItemJquery.find('.hostValue').text(pool.host);
	this.poolItemJquery.find('.usernameValue').text(pool.username);
	this.poolItemJquery.find('.passwordValue').text(pool.password);
	this.poolItemJquery.find('.isActiveValue').text(pool.isActive);
	this.poolItemJquery.find('.isEnabledValue').text(pool.isEnabled);
	this.poolItemJquery.find('.isStableValue').text(pool.isStable);
	this.poolItemJquery.find('.isActiveSinceValue').text(
			pool.isActiveSince != undefined ? pool.isActiveSince : "Never");
	this.poolItemJquery.find('.difficultyValue').text(
			pool.difficulty != undefined ? pool.difficulty : "Not set");
	this.poolItemJquery.find('.extranonce1Value').text(
			pool.extranonce1 != undefined ? pool.extranonce1 : "Not set");
	this.poolItemJquery.find('.extranonce2SizeValue').text(
			pool.extranonce2Size != undefined ? pool.extranonce2Size
					: "Not set");
	this.poolItemJquery
			.find('.workerExtranonce2SizeValue')
			.text(
					pool.workerExtranonce2Size != undefined ? pool.workerExtranonce2Size
							: "Not set");
	this.poolItemJquery.find('.numberOfWorkersValue').text(
			pool.numberOfWorkerConnections);
	this.poolItemJquery.find('.priorityValue').text(pool.priority);
	this.poolItemJquery.find('.isExtranonceSubscribeEnabledValue').text(
			pool.isExtranonceSubscribeEnabled);
	this.poolItemJquery.find('.acceptedDifficultyValue').text(
			pool.acceptedDifficulty);
	this.poolItemJquery.find('.rejectedDifficultyValue').text(
			pool.rejectedDifficulty);
	this.poolItemJquery.find('.acceptedHashrateValue').text(
			pool.acceptedHashesPerSeconds);
	this.poolItemJquery.find('.rejectedHashrateValue').text(
			pool.rejectedHashesPerSeconds);

	// Associate the pool priority to the jQuery object to allow sorting of
	// pools.
	this.poolItemJquery.data('priority', pool.priority);

	// Apply the color of the panel header based on the pool status
	// By default, the color is white (panel-default). This color is the
	// disabled pool color.
	this.poolItemJquery.removeClass('panel-default');
	this.poolItemJquery.removeClass('panel-danger');
	this.poolItemJquery.removeClass('panel-primary');
	this.poolItemJquery.removeClass('panel-warning');
	this.poolItemJquery.removeClass('panel-success');
	if (pool.isEnabled) {
		if (!pool.isActive) {
			this.poolItemJquery.addClass('panel-danger');
		} else {
			if (pool.isMining) {
				this.poolItemJquery.addClass('panel-primary');
			} else {
				if (!pool.isStable) {
					this.poolItemJquery.addClass('panel-warning');
				} else {
					this.poolItemJquery.addClass('panel-success');
				}
			}
		}
	} else {
		this.poolItemJquery.addClass('panel-default');
	}

	// Update the buttons state of the item

	// Update the enableDisable button
	if (pool.isEnabled) {
		this.getEnableDisableButton().text("Disable");
		this.getEnableDisableButton().removeClass("btn-warning").removeClass(
				"btn-success");
		this.getEnableDisableButton().addClass("btn-warning");
	} else {
		this.getEnableDisableButton().text("Enable");
		this.getEnableDisableButton().removeClass("btn-warning").removeClass(
				"btn-success");
		this.getEnableDisableButton().addClass("btn-success");
	}

};

PoolItem.prototype.remove = function() {
	this.poolItemJquery.remove();
};

PoolItem.prototype.getEditButton = function() {
	return this.poolItemJquery.find(".edit");
};

PoolItem.prototype.getSetHighestPriorityButton = function() {
	return this.poolItemJquery.find(".setHighestPriority");
};

PoolItem.prototype.getEnableDisableButton = function() {
	return this.poolItemJquery.find(".enableDisable");
};

PoolItem.prototype.getRemoveButton = function() {
	return this.poolItemJquery.find(".remove");
};

/**
 * Reload the data of the hashrate chart. If isUpdate is true, just update the
 * graph with new data. Else replace all data.
 */
PoolItem.prototype.reloadChartData = function(isUpdate) {
	var poolItem = this;
	$.ajax({
		url : "/proxy/hashrate/pool",
		dataType : "json",
		type : "POST",
		data : JSON.stringify({
			poolName : this.pool.name
		}),
		contentType : "application/json",
		success : function(data) {
			// When pools are retrieved, create the items
			if (data != undefined && data.hashrates != undefined) {
				// Initialize the chart if not already done.
				if (poolItem.chart == undefined) {
					poolItem.initChart();
				}

				// If it is not an update, load the full data
				if (!isUpdate) {
					poolItem.setHashrateChartData(data.hashrates);
				} else {
					// If it is an update, only happen the missing data.
					poolItem.updateHashrateChartData(data.hashrates);
				}
			}
		},
		error : function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window.alert('Failed to get hashrates for pool '
					+ poolItem.pool.name + '. Status: ' + textStatus
					+ ', error: ' + errorThrown + ', message: '
					+ jsonObject.message);
		}
	});
};

/**
 * Set the given hashrates in the chart. Replace all existing data.
 */
PoolItem.prototype.setHashrateChartData = function(hashrates) {
	var acceptedData = new Array();
	var rejectedData = new Array();

	hashrates.forEach(function(hashrate) {
		var time = hashrate.captureTimeUTC * 1000, hashrateData;
		hashrateData = [ time, hashrate.acceptedHashrate ];
		acceptedData.push(hashrateData);

		hashrateData = [ time, hashrate.rejectedHashrate ];
		rejectedData.push(hashrateData);
	});

	this.chart.series[0].setData(acceptedData);
	this.chart.series[1].setData(rejectedData);

	// Fix a bug in HighStock: The rangeSelector is not displayed on initial
	// draw. It is only displayed if the char tis resized.
	var extremes = this.chart.xAxis[0].getExtremes();
	this.chart.rangeSelector.render(extremes.min, extremes.max);
};

/**
 * Merge the given hashrates with the ones already present in the graph. Just
 * happens the new ones.
 */
PoolItem.prototype.updateHashrateChartData = function(hashrates) {

	var maxTime = this.chart.xAxis[0].getExtremes().dataMax;
	// Check all newest values and add them if they are
	// newer
	// than the max already present.
	for (var i = hashrates.length - 1; i >= 0; i--) {
		var time = hashrates[i].captureTimeUTC * 1000;
		if (time > maxTime) {
			this.chart.series[0]
					.addPoint([ time, hashrates[i].acceptedHashrate ]);
			this.chart.series[1]
					.addPoint([ time, hashrates[i].rejectedHashrate ]);
		} else {
			break;
		}
	}
};

/*
 * Global variables
 */
// Store the pages controllers
var pagesControllers = new Array();

// Store the id of the next pool item to generate
var nextPoolItemId = 0;

// The default auto-refresh delay is 1 minute
var autoRefreshDelay = 60000;

/*
 * The main application
 */
$(function() {
	launchClient();
});

/**
 * Launch the client.
 */
function launchClient() {
	initToTopScroller();
	
	initBootstrapSelect();

	initHighcharts();

	initControllers();

	initNavBarHandlers();
}

/**
 * Init the to top/bottom scroller plugin
 */
var toTopBottomScroller;
function initToTopScroller() {
	toTopBottomScroller = $('#totopscroller').totopscroller({
		showToBottom : true,
		link : false,
		linkTarget : '_self',
		toTopHtml : '<a href="#"></a>',
		toBottomHtml : '<a href="#"></a>',
		toPrevHtml : '<a href="#"></a>',
		linkHtml : '<a href="#"></a>',
		toTopClass : 'totopscroller-top',
		toBottomClass : 'totopscroller-bottom',
		toPrevClass : 'totopscroller-prev',
		linkClass : 'totopscroller-lnk',
	});
	
	$(document).ajaxComplete(function() {
		refreshToTopScroller();
	});
}

/**
 * Refresh the to Top/Bottom scroller
 */
function refreshToTopScroller() {
	toTopBottomScroller.refresh();
}

/**
 * Initialize the bootstrap-select plugin
 */
function initBootstrapSelect() {
	$('.selectpicker').selectpicker();
}

/**
 * Initialize highcharts global options
 */
function initHighcharts() {
	Highcharts.setOptions({
		global : {
			useUTC : false
		}
	});
}

/**
 * Initialize the page controllers
 */
function initControllers() {
	var poolPageController = new PoolsPageController('poolsPage');
	pagesControllers.push(poolPageController);
	pagesControllers.push(new PageController('usersPage'));
	pagesControllers.push(new PageController('connectionsPage'));
	pagesControllers.push(new PageController('settingsPage'));
	pagesControllers.push(new LogsPageController('logsPage'));

	poolPageController.load();
}

/**
 * Load the controller with the given page name.
 */
function loadPageController(pageName) {
	pagesControllers.forEach(function(controller) {
		if (controller.pageName == pageName) {
			controller.unload();
			controller.load();
		} else {
			controller.unload();
		}
	});
}

/**
 * Return the controller of the given page name
 * 
 * @param pageName
 */
function getControllerOfPage(pageName) {
	var result = null;
	pagesControllers.forEach(function(controller) {
		if (controller.pageName == pageName) {
			result = controller;
		}
	});
	return result;
}

/**
 * Initialize the navigation bar handler
 */
function initNavBarHandlers() {
	$('#navbarUl a').click(function(e) {
		onNavbarButtonSelection(this);
	});
}

/**
 * Process the navbar button selection event
 */
function onNavbarButtonSelection(navbarButton) {
	var jqueryButton = $(navbarButton);

	// Remove the active of all other buttons
	jqueryButton.parent().parent().children().removeClass();
	// Then set the active button
	jqueryButton.parent().addClass('active');

	// Then show the selected page.
	loadPageController(jqueryButton.attr('page-name'));
}

/**
 * Create an empty pool item
 */
function createPoolItem() {
	var newItem = $('#templatePoolItem').clone();
	newItem.attr('id', 'templatePoolItem' + nextPoolItemId++);
	newItem.removeAttr('style');
	return newItem;
}

/*
 * Utils
 */
Array.prototype.clear = function() {
	while (this.length > 0) {
		this.pop();
	}
};

Array.prototype.find = function(predicate) {
	var result = null;
	this.forEach(function(data) {
		if (predicate(data)) {
			result = data;
		}
	});
	return result;
};

Array.prototype.removeItem = function(item) {
	var deleted = null, index = 0;
	while ((index = this.indexOf(item, index)) != -1) {
		deleted = this.splice(index, 1);
	}
	if (deleted) {
		return deleted[0];
	}
};

/**
 * jQuery.fn.sort --------------
 * 
 * @author James Padolsey (http://james.padolsey.com)
 * @version 0.1
 * @updated 18-MAR-2010 --------------
 * @param Function
 *            comparator: Exactly the same behaviour as [1,2,3].sort(comparator)
 * 
 * @param Function
 *            getSortable A function that should return the element that is to
 *            be sorted. The comparator will run on the current collection, but
 *            you may want the actual resulting sort to occur on a parent or
 *            another associated element.
 * 
 * E.g. $('td').sort(comparator, function(){ return this.parentNode; })
 * 
 * The
 * <td>'s parent (
 * <tr>) will be sorted instead of the
 * <td> itself.
 */
jQuery.fn.sort = (function() {

	var sort = [].sort;

	return function(comparator, getSortable) {

		getSortable = getSortable || function() {
			return this;
		};

		var placements = this
				.map(function() {

					var sortElement = getSortable.call(this), parentNode = sortElement.parentNode,

					// Since the element itself will change position, we have
					// to have some way of storing it's original position in
					// the DOM. The easiest way is to have a 'flag' node:
					nextSibling = parentNode.insertBefore(document
							.createTextNode(''), sortElement.nextSibling);

					return function() {

						if (parentNode === this) {
							throw new Error(
									"You can't sort elements if any one is a descendant of another.");
						}

						// Insert before flag:
						parentNode.insertBefore(this, nextSibling);
						// Remove flag:
						parentNode.removeChild(nextSibling);

					};

				});

		return sort.call(this, comparator).each(function(i) {
			placements[i].call(getSortable.call(this));
		});

	};

})();
