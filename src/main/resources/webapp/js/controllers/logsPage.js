define(['jquery', 'ractivejs', 'controllers/abstractPageController', 'rv!templates/logsPage', 'i18n!locales',
	'config', 'json'], function($, Ractive, AbstractPageController, template, i18next, config) {

    var LogsPageController = function(pageName) {
	AbstractPageController.call(this, pageName);

	this.isFirstLoad = true;
    };

    LogsPageController.prototype = Object.create(AbstractPageController.prototype);
    LogsPageController.prototype.constructor = LogsPageController;

    /**
     * Load the logs page
     */
    LogsPageController.prototype.onLoad = function(mainContainer) {
	var controller = this;

	this.ractive = new Ractive({
	    el: mainContainer,
	    template: template,
	    data: {
		autoScrollCheckbox: false
	    }
	});

	mainContainer.i18n();

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
	var selectPicker = this.getContainer().find('.logLevelSelectPicker');
	var controller = this;
	selectPicker.prop('disabled', true);
	selectPicker.selectpicker('refresh');

	$.ajax({
	    url: '/proxy/log/level',
	    dataType: "json",
	    type: "GET",
	    contentType: "application/json",
	    success: function(data) {
		if (data != undefined) {
		    selectPicker.selectpicker('val', data.logLevel);
		    selectPicker.prop('disabled', false);
		    selectPicker.selectpicker('refresh');
		    controller.initLogLevelSelect();
		}
	    },
	    error: function(request, textStatus, errorThrown) {
		window
			.alert('Failed to get the log level. Status: ' + textStatus + ', error: '
				+ errorThrown);
	    }
	});
    };

    /**
     * Initialize the select input to change the log level
     */
    LogsPageController.prototype.initLogLevelSelect = function() {
	var selectPicker = this.getContainer().find('.logLevelSelectPicker');
	selectPicker.off('change');
	selectPicker.change(function() {
	    var selectedValue = selectPicker.find('option:selected').text();
	    selectPicker.selectpicker('refresh');
	    $.ajax({
		url: '/proxy/log/level',
		dataType: "json",
		type: "POST",
		data: JSON.stringify({
		    logLevel: selectedValue
		}),
		contentType: "application/json",
		error: function(request, textStatus, errorThrown) {
		    window.alert('Failed to change the log level. Status: ' + textStatus + ', error: '
			    + errorThrown);
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
	    url: '/proxy/log/since',
	    dataType: "json",
	    type: "POST",
	    data: JSON.stringify({
		timestamp: controller.lastLogLineTimestamp
	    }),
	    contentType: "application/json",
	    success: function(data) {
		if (data != undefined) {
		    controller.appendLogs(data);
		}
	    },
	    error: function(request, textStatus, errorThrown) {
		var jsonObject = JSON.parse(request.responseText);
		window.alert('Failed to get the last log lines. Status: ' + textStatus + ', error: '
			+ errorThrown + ', message: ' + jsonObject.message);
	    }
	});
    };

    /**
     * Clear the logs panel
     */
    LogsPageController.prototype.clear = function() {
	this.getContainer().find('.logsContainer').text("");
	this.lastLogLineTimestamp = null;
    };

    /**
     * Append the given logs data.
     */
    LogsPageController.prototype.appendLogs = function(logsData) {
	var logString = "", controller = this;

	logsData.forEach(function(logEntry) {
	    controller.lastLogLineTimestamp = logEntry.key;
	    logString += logEntry.value;
	});

	this.getContainer().find('.logsContainer').append(logString);

	if (this.ractive.get('autoScrollCheckbox') && logsData.length > 0) {
	    $('html, body').animate({
		scrollTop: $(document).height()
	    });
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

    return LogsPageController;

});
