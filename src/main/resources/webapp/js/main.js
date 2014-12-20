require.config({
    paths: {
	'jquery': 'libs/jquery-2.1.1.min',
	'json': 'libs/json2',
	'bootstrap': 'libs/bootstrap.min',
	'highstock': 'libs/highstock',

	// bootstrap plugin to format ComboBox
	'bootstrap-select': 'libs/bootstrap-select.min',

	// Jquery plugin to add a ToTopBottom button
	'totop': 'libs/jquery-plugins/jquery-totop',

	// Jquery plugin to allow element sorting on jquery collections
	'sort': 'libs/jquery-plugins/jquery-sort',

	// requirejs text plugin
	'text': 'libs/requirejs-plugins/text',

	// requirejs ractive plugin
	'rv': 'libs/requirejs-plugins/rv',
	
	// requirejs i18n plugin
	'i18n': 'libs/requirejs-plugins/i18next',
	
	// The i18next library
	'i18next': 'libs/i18next.amd.withJQuery',

	// used for optimization of the rv plugin
	'ractive': 'libs/ractive.min',

	// runtime version of ractive
	'ractivejsWithoutDefaults': 'libs/ractive.runtime.min',

	// return the Ractive module with the custom default options (to use in
	// modules)
	'ractivejs': 'libs/ractive.customDefaults',
	
	'ui': '../ui',
	'templates': '../ui/templates',
	'controllers': 'controllers',
	'locales': '../locales'
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
	},
	'json': {
	    exports: 'JSON'
	}
    },
    stubModules: ['rv'],
    i18next: {
        fallbackLng  : "en",
        fallbackOnNull: true,
        lowerCaseLng : true,
        resGetPath   : "__lng__/__ns__.json",
        ns: "stratumProxy"
    }

});

// Start the application
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