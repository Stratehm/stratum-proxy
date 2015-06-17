define(['jquery', 'ractivejs', 'rv!templates/userItem', 'i18n!locales', 'config', 'managers/authenticationManager','highstock'], function($,
	Ractive, template, i18next, config, authenticationManager) {

    var UserItem = function(renderToElement) {
	var userItemId = UserItem.nextUserItemId++;
	this.ractive = new Ractive({
	    el: renderToElement,
	    template: template,
	    data: {
		userItemId: userItemId
	    },
	    oncomplete: $.proxy(function() {
		this.userItemJquery.i18n();
	    }, this)
	});
	this.userItemJquery = $('#userItem-' + userItemId);

	var self = this;
	document.addEventListener('localeChanged', function() {
	    self.reloadChartData(false, true);
	}, false);
	
	this.updateAccessibleItems();
	document.addEventListener('loginSuccess', function() {
	    self.updateAccessibleItems();
	}, false);
    };

    UserItem.nextUserItemId = 0;

    UserItem.prototype.setUser = function(user) {
	// Update the values in the panel
	this.updateUser(user);

	// Initialize a tooltip when the text overflows
	this.userItemJquery.find('.tooltipOnOverflow').bind('mouseenter', function() {
	    var $this = $(this);
	    if (this.offsetWidth < this.scrollWidth && !$this.attr('title')) {
		$this.attr('title', $this.text());
	    }
	});

	// Reload the data of the chart
	this.reloadChartData(false);
    };

    UserItem.prototype.initChart = function() {
	// Remove the loader
	this.userItemJquery.find(".hashrateChart").empty();

	// Create the chart
	this.userItemJquery.find(".hashrateChart").highcharts('StockChart', {
	    chart: {
		zoomType: 'x'
	    },
	    rangeSelector: {
		enabled: true,
		inputEnabled: false,
		buttons: [{
		    type: 'hour',
		    count: 1,
		    text: i18next.t('userItem.chart.zoom1Hour')
		}, {
		    type: 'day',
		    count: 1,
		    text: i18next.t('userItem.chart.zoom1Day')
		}, {
		    type: 'week',
		    count: 1,
		    text: i18next.t('userItem.chart.zoom1Week')
		}, {
		    type: 'month',
		    count: 1,
		    text: i18next.t('userItem.chart.zoom1Month')
		}, {
		    type: 'year',
		    count: 1,
		    text: i18next.t('userItem.chart.zoom1Year')
		}, {
		    type: 'all',
		    text: i18next.t('userItem.chart.zoomAll')
		}]
	    },
	    navigator: {
		enabled: false
	    },
	    scrollbar: {
		enabled: false
	    },
	    title: {
		text: i18next.t('userItem.chart.title')
	    },
	    xAxis: {
		type: 'datetime',
		title: {
		    text: i18next.t('userItem.chart.xAxisTitle')
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
		    text: i18next.t('userItem.chart.yAxisTitle')
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
		name: i18next.t('userItem.chart.tooltipAcceptedHashrate'),
		type: 'area'
	    }, {
		name: i18next.t('userItem.chart.tooltipRejectedHashrate'),
		type: 'area'
	    }]
	});

	// Associate the chart with the UserItem
	this.chart = this.userItemJquery.find(".hashrateChart").highcharts();
    };

    UserItem.prototype.updateUser = function(user) {
	this.user = user;

	// Apply the color of the panel header based on the user status
	// By default, the color is white (panel-default). This color is the
	// inactive user color.
	var panelHeaderLevel = 'panel-default';
	if (user.connections == null || user.connections.length < 1) {
	    user.isActive = false;
	    panelHeaderLevel = 'panel-danger';
	} else {
	    user.isActive = true;
	    panelHeaderLevel = 'panel-success';
	}

	this.ractive.set(user);
	this.ractive.set({
	    panelHeaderLevel: panelHeaderLevel
	});
	
	this.userItemJquery.data('user', user);
    };

    UserItem.prototype.remove = function() {
	this.userItemJquery.remove();
    };

    UserItem.prototype.getKickButton = function() {
	return this.userItemJquery.find(".kickUser");
    };

    UserItem.prototype.getBanButton = function() {
	return this.userItemJquery.find(".banUser");
    };

    /**
     * Reload the data of the hashrate chart. If isUpdate is true, just update
     * the graph with new data. Else replace all data. If redraw is true, redraw
     * the chart.
     */
    UserItem.prototype.reloadChartData = function(isUpdate, redraw) {
	var userItem = this;
	$.ajax({
	    url: "proxy/hashrate/user",
	    dataType: "json",
	    type: "POST",
	    data: JSON.stringify({
		username: this.user.name
	    }),
	    contentType: "application/json",
	    success: function(data) {
		// When users hashrates are retrieved, create the graph
		if (data != undefined && data.hashrates != undefined) {
		    // Initialize the chart if not already done.
		    if (userItem.chart == undefined || redraw) {
			userItem.initChart();
		    }

		    // If it is not an update, load the full data
		    if (!isUpdate) {
			userItem.setHashrateChartData(data.hashrates);
		    } else {
			// If it is an update, only happen the missing data.
			userItem.updateHashrateChartData(data.hashrates);
		    }
		}
	    },
	    error: function(request, textStatus, errorThrown) {
		var jsonObject = JSON.parse(request.responseText);
		window.alert('Failed to get hashrates for user ' + userItem.user.name + '. Status: '
			+ textStatus + ', error: ' + errorThrown + ', message: ' + jsonObject.message);
	    }
	});
    };

    /**
     * Set the given hashrates in the chart. Replace all existing data.
     */
    UserItem.prototype.setHashrateChartData = function(hashrates) {
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

	// Fix a bug in HighStock: The rangeSelector is not displayed on initial
	// draw. It is only displayed if the char tis resized.
	var extremes = this.chart.xAxis[0].getExtremes();
	this.chart.rangeSelector.render(extremes.min, extremes.max);
    };

    /**
     * Merge the given hashrates with the ones already present in the graph.
     * Just happens the new ones.
     */
    UserItem.prototype.updateHashrateChartData = function(hashrates) {

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
    
    /**
     * Update the displayed items based on the authorization.
     */
    UserItem.prototype.updateAccessibleItems = function() {
	if(!authenticationManager.isAuthenticated) {
	    this.userItemJquery.find('.kickUser, .banUser').hide();
	} else {
	    this.userItemJquery.find('.kickUser, .banUser').show();
	}
    };

    return UserItem;
});