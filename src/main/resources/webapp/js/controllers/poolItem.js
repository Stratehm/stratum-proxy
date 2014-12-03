define(['jquery', 'text!templates/poolItem.handlebars', 'config', 'highstock'],
	function($, template, config) {

	    var PoolItem = function() {
		this.poolItemJquery = $(template);
		this.poolItemJquery.attr('id', 'templatePoolItem' + PoolItem.nextPoolItemId++);
	    };
	    
	    PoolItem.prototype.nextPoolItemId = 0;

	    PoolItem.prototype.setPool = function(pool) {
		// Update the values in the panel
		this.updatePool(pool);

		// Initialize a tooltip when the text overflows
		this.poolItemJquery.find('.tooltipOnOverflow').bind('mouseenter', function() {
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
		this.poolItemJquery.find(".hashrateChart").highcharts('StockChart', {
		    chart: {
			zoomType: 'x'
		    },
		    rangeSelector: {
			enabled: true,
			inputEnabled: false,
			buttons: [{
			    type: 'hour',
			    count: 1,
			    text: '1h'
			}, {
			    type: 'day',
			    count: 1,
			    text: '1d'
			}, {
			    type: 'week',
			    count: 1,
			    text: '1w'
			}, {
			    type: 'day',
			    count: 7,
			    text: '1m'
			}, {
			    type: 'month',
			    count: 1,
			    text: '1y'
			}, {
			    type: 'all',
			    text: 'All'
			}]
		    },
		    navigator: {
			enabled: false
		    },
		    scrollbar: {
			enabled: false
		    },
		    title: {
			text: 'Hashrate'
		    },
		    xAxis: {
			type: 'datetime',
			title: {
			    text: 'Date'
			},
			ordinal: false
		    },
		    yAxis: [{
			min: 0,
			labels: {
			    align: 'left'
			}
		    }, {
			linkedTo: 0,
			opposite: false,
			title: {
			    text: 'Hashrate'
			}
		    }],
		    tooltip: {
			shared: true,
			positioner: function(labelWidth, labelHeight, point) {
			    var xPos = point.plotX + this.chart.plotLeft + 10;
			    var yPos = point.plotY + this.chart.plotTop;

			    if (point.plotX + labelWidth > this.chart.chartWidth - this.chart.plotLeft) {
				xPos = point.plotX + this.chart.plotLeft - labelWidth - 10;
			    }

			    return {
				x: xPos,
				y: yPos
			    };
			}
		    },
		    plotOptions: {
			area: {
			    stacking: 'normal',
			    lineWidth: 1,
			    marker: {
				enabled: false
			    }
			}
		    },
		    series: [{
			name: 'Accepted',
			type: 'area'
		    }, {
			name: 'Rejected',
			type: 'area'
		    }]
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
		this.poolItemJquery.find('.isReadyValue').text(pool.isReady);
		this.poolItemJquery.find('.isActiveValue').text(pool.isActive);
		this.poolItemJquery.find('.isEnabledValue').text(pool.isEnabled);
		this.poolItemJquery.find('.isStableValue').text(pool.isStable);
		this.poolItemJquery.find('.isReadySinceValue').text(
			pool.isReadySince != undefined ? pool.isReadySince : "Never");
		this.poolItemJquery.find('.isActiveSinceValue').text(
			pool.isActiveSince != undefined ? pool.isActiveSince : "Not active");
		this.poolItemJquery.find('.difficultyValue').text(
			pool.difficulty != undefined ? pool.difficulty : "Not set");
		this.poolItemJquery.find('.extranonce1Value').text(
			pool.extranonce1 != undefined ? pool.extranonce1 : "Not set");
		this.poolItemJquery.find('.extranonce2SizeValue').text(
			pool.extranonce2Size != undefined ? pool.extranonce2Size : "Not set");
		this.poolItemJquery.find('.workerExtranonce2SizeValue').text(
			pool.workerExtranonce2Size != undefined ? pool.workerExtranonce2Size : "Not set");
		this.poolItemJquery.find('.numberOfWorkersValue').text(pool.numberOfWorkerConnections);
		this.poolItemJquery.find('.priorityValue').text(pool.priority);
		this.poolItemJquery.find('.weightValue').text(pool.weight);
		this.poolItemJquery.find('.isExtranonceSubscribeEnabledValue').text(
			pool.isExtranonceSubscribeEnabled);
		this.poolItemJquery.find('.acceptedDifficultyValue').text(pool.acceptedDifficulty);
		this.poolItemJquery.find('.rejectedDifficultyValue').text(pool.rejectedDifficulty);
		this.poolItemJquery.find('.acceptedHashrateValue').text(pool.acceptedHashesPerSeconds);
		this.poolItemJquery.find('.rejectedHashrateValue').text(pool.rejectedHashesPerSeconds);

		this.poolItemJquery.find('.lastStopCauseValue').text(
			pool.lastStopCause != undefined ? pool.lastStopCause : "Never stopped");
		this.poolItemJquery.find('.lastStopDateValue').text(
			pool.lastStopDate != undefined ? pool.lastStopDate : "Never stopped");

		this.poolItemJquery.find('.appendWorkersNamesValue').text(pool.appendWorkerNames);
		this.poolItemJquery.find('.workerNameSeparatorValue').text(pool.workerNamesSeparator);
		this.poolItemJquery.find('.useWorkerPasswordValue').text(pool.useWorkerPassword);

		// Associate the pool priority to the jQuery object to allow
		// sorting of
		// pools.
		this.poolItemJquery.data('priority', pool.priority);

		// Apply the color of the panel header based on the pool status
		// By default, the color is white (panel-default). This color is
		// the
		// disabled pool color.
		this.poolItemJquery.removeClass('panel-default');
		this.poolItemJquery.removeClass('panel-danger');
		this.poolItemJquery.removeClass('panel-primary');
		this.poolItemJquery.removeClass('panel-warning');
		this.poolItemJquery.removeClass('panel-success');
		if (pool.isEnabled) {
		    if (!pool.isReady) {
			this.poolItemJquery.addClass('panel-danger');
		    } else {
			if (pool.isActive) {
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
		    this.getEnableDisableButton().removeClass("btn-warning").removeClass("btn-success");
		    this.getEnableDisableButton().addClass("btn-warning");
		} else {
		    this.getEnableDisableButton().text("Enable");
		    this.getEnableDisableButton().removeClass("btn-warning").removeClass("btn-success");
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
	     * Reload the data of the hashrate chart. If isUpdate is true, just
	     * update the graph with new data. Else replace all data.
	     */
	    PoolItem.prototype.reloadChartData = function(isUpdate) {
		var poolItem = this;
		$.ajax({
		    url: "/proxy/hashrate/pool",
		    dataType: "json",
		    type: "POST",
		    data: JSON.stringify({
			poolName: this.pool.name
		    }),
		    contentType: "application/json",
		    success: function(data) {
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
				// If it is an update, only happen the missing
				// data.
				poolItem.updateHashrateChartData(data.hashrates);
			    }
			}
		    },
		    error: function(request, textStatus, errorThrown) {
			var jsonObject = JSON.parse(request.responseText);
			window
				.alert('Failed to get hashrates for pool ' + poolItem.pool.name
					+ '. Status: ' + textStatus + ', error: ' + errorThrown
					+ ', message: ' + jsonObject.message);
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
		    hashrateData = [time, hashrate.acceptedHashrate];
		    acceptedData.push(hashrateData);

		    hashrateData = [time, hashrate.rejectedHashrate];
		    rejectedData.push(hashrateData);
		});

		this.chart.series[0].setData(acceptedData);
		this.chart.series[1].setData(rejectedData);

		// Fix a bug in HighStock: The rangeSelector is not displayed on
		// initial
		// draw. It is only displayed if the char tis resized.
		var extremes = this.chart.xAxis[0].getExtremes();
		this.chart.rangeSelector.render(extremes.min, extremes.max);
	    };

	    /**
	     * Merge the given hashrates with the ones already present in the
	     * graph. Just happens the new ones.
	     */
	    PoolItem.prototype.updateHashrateChartData = function(hashrates) {

		var maxTime = this.chart.xAxis[0].getExtremes().dataMax;
		// Check all newest values and add them if they are
		// newer
		// than the max already present.
		for (var i = hashrates.length - 1; i >= 0; i--) {
		    var time = hashrates[i].captureTimeUTC * 1000;
		    if (time > maxTime) {
			this.chart.series[0].addPoint([time, hashrates[i].acceptedHashrate]);
			this.chart.series[1].addPoint([time, hashrates[i].rejectedHashrate]);
		    } else {
			break;
		    }
		}
	    };

	    return PoolItem;

	});