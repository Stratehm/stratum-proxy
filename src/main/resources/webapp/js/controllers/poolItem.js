define(['jquery', 'ractivejs', 'rv!templates/poolItem', /*'i18n!locales/poolItem',*/ 'config', 'highstock'], function($, Ractive,
	template, /*locale,*/ config) {

    var PoolItem = function(renderToElement) {
	var poolItemId = PoolItem.nextPoolItemId++;
	this.ractive = new Ractive({
	    el: renderToElement,
	    template: template,
	    data: {
		poolItemId: poolItemId
	    }
	});
	this.poolItemJquery = $('#poolItem-' + poolItemId);

	this.updateLabels();
    };

    PoolItem.prototype.updateLabels = function() {
	// Set the i18n labels
	this.ractive.set({
	    /*hostLabel: locale.hostLabel,
	    usernameLabel: locale.usernameLabel,
	    passwordLabel: locale.passwordLabel,
	    difficultyLabel: locale.difficultyLabel,
	    isEnabledLabel: locale.isEnabledLabel,
	    isStableLabel: locale.isStableLabel,
	    isReadyLabel: locale.isReadyLabel,
	    isReadySinceLabel: locale.isReadySinceLabel,
	    isActiveLabel: locale.isActiveLabel,
	    isActiveSinceLabel: locale.isActiveSinceLabel,
	    extranonce1Label: locale.extranonce1Label,
	    extranonce2SizeLabel: locale.extranonce2SizeLabel,
	    workerExtranonce2SizeLabel: locale.workerExtranonce2SizeLabel,
	    numberOfWorkersLabel: locale.numberOfWorkersLabel,
	    priorityLabel: locale.priorityLabel,
	    weightLabel: locale.weightLabel,
	    lastStopCauseLabel: locale.lastStopCauseLabel,
	    lastStopDateLabel: locale.lastStopDateLabel,
	    isExtranonceSubscribeEnabledLabel: locale.isExtranonceSubscribeEnabledLabel,
	    isAppendWorkerNameLabel: locale.isAppendWorkerNameLabel,
	    workerNameSeparatorLabel: locale.workerNameSeparatorLabel,
	    isUseWorkerPasswordLabel: locale.isUseWorkerPasswordLabel,
	    acceptedDifficultyLabel: locale.acceptedDifficultyLabel,
	    rejectedDifficultyLabel: locale.rejectedDifficultyLabel,
	    acceptedHashrateLabel: locale.acceptedHashrateLabel,
	    rejectedHashrateLabel: locale.rejectedHashrateLabel,

	    setHighestPriorityButton: locale.setHighestPriorityButton,
	    disableButton: locale.disableButton,
	    enableButton: locale.enableButton,
	    removeButton: locale.removeButton,
	    editButton: locale.editButton,

	    noIsReadySince: locale.noIsReadySince,
	    noIsActiveSince: locale.noIsActiveSince,
	    noDifficulty: locale.noDifficulty,
	    noExtranonce1: locale.noExtranonce1,
	    noExtranonce2Size: locale.noExtranonce2Size,
	    noWorkerExtranonce2Size: locale.noWorkerExtranonce2Size,
	    noLastStopCause: locale.noLastStopCause,
	    noLastStopDate: locale.noLastStopDate*/
	    hostLabel: 'Host',
	    usernameLabel: 'Username',
	    passwordLabel: 'Password',
	    difficultyLabel: 'Difficulty',
	    isEnabledLabel: 'Is enabled',
	    isStableLabel: 'Is stable',
	    isReadyLabel: 'Is ready',
	    isReadySinceLabel: 'Is ready since',
	    isActiveLabel: 'Is active',
	    isActiveSinceLabel: 'Is active since',
	    extranonce1Label: 'Extranonce 1',
	    extranonce2SizeLabel: 'Extranonce 2 size',
	    workerExtranonce2SizeLabel: 'Worker extranonce 2 size',
	    numberOfWorkersLabel: 'Number of workers',
	    priorityLabel: 'Priority',
	    weightLabel: 'Weight',
	    lastStopCauseLabel: 'Last stop cause',
	    lastStopDateLabel: 'Last stop date',
	    isExtranonceSubscribeEnabledLabel: 'Is extranonce subscribe	enabled',
	    isAppendWorkerNameLabel: 'Append worker\'s name',
	    workerNameSeparatorLabel: 'Worker name separator',
	    isUseWorkerPasswordLabel: 'Use worker\'s password',
	    acceptedDifficultyLabel: 'Accepted difficulty',
	    rejectedDifficultyLabel: 'Rejected difficulty',
	    acceptedHashrateLabel: 'Accepted hashrate',
	    rejectedHashrateLabel: 'Rejected hashrate',

	    setHighestPriorityButton: 'Set highest priority',
	    disableButton: 'Disable',
	    enableButton: 'Enable',
	    removeButton: 'Remove',
	    editButton: 'Edit',

	    noIsReadySince: 'Never',
	    noIsActiveSince: 'Never',
	    noDifficulty: 'Not set',
	    noExtranonce1: 'Not set',
	    noExtranonce2Size: 'Not set',
	    noWorkerExtranonce2Size: 'Not set',
	    noLastStopCause: 'Never stopped',
	    noLastStopDate: 'Never stopped'
	});
	
    };

    PoolItem.nextPoolItemId = 0;

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

	// The the pool values
	this.ractive.set(pool);

	// Associate the pool priority to the jQuery object to allow
	// sorting of
	// pools.
	this.poolItemJquery.data('priority', pool.priority);

	// Apply the color of the panel header based on the pool status
	// By default, the color is white (panel-default). This color is
	// the disabled pool color.
	var panelHeaderLevel = 'panel-default';
	if (pool.isEnabled) {
	    if (!pool.isReady) {
		panelHeaderLevel = 'panel-danger';
	    } else {
		if (pool.isActive) {
		    panelHeaderLevel = 'panel-primary';
		} else {
		    if (!pool.isStable) {
			panelHeaderLevel = 'panel-warning';
		    } else {
			panelHeaderLevel = 'panel-success';
		    }
		}
	    }
	}
	this.ractive.set('panelHeaderLevel', panelHeaderLevel);
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
     * Reload the data of the hashrate chart. If isUpdate is true, just update
     * the graph with new data. Else replace all data.
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
		window.alert('Failed to get hashrates for pool ' + poolItem.pool.name + '. Status: '
			+ textStatus + ', error: ' + errorThrown + ', message: ' + jsonObject.message);
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
     * Merge the given hashrates with the ones already present in the graph.
     * Just happens the new ones.
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