require.config({
    paths: {
	'jquery': 'libs/jquery-2.1.1.min',
	'json2': 'libs/json2',
	'bootstrap': 'libs/bootstrap.min',
	'bootstrap-select': 'libs/bootstrap-select.min',
	'totop': 'libs/jquery-totop',
	'sort': 'libs/jquery-sort',
	'highstock': 'libs/highstock',
	'handlebars': 'libs/handlebars-v2.0.0',
	'text': 'libs/text',
	'ui': '../ui',
	'templates': '../ui/templates',
	'controllers': 'controllers'
    },
    shim: {
	'bootstrap': {
	    deps: ['jquery']
	},
	'bootstrap-select': {
	    deps: ['bootstrap']
	},
	'totop': {
	    deps: ['jquery']
	},
	'sort': {
	    deps: ['jquery']
	},
	'highstock': {
	    deps: ['jquery'],
	    exports: 'Highcharts'
	}
    }

});

require(['stratum-proxy-client'], function(client) {
    client.launchClient();
});

/**
 * Array utils
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