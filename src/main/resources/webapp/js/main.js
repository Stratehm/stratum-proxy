require.config({
    paths: {
	'jquery': 'libs/jquery-2.1.3',
	'json': 'libs/json2',
	'bootstrap': 'libs/bootstrap',
	'highstock': 'libs/highstock.src',
	'moment': 'libs/moment-with-locales',

	// bootstrap plugin to format ComboBox
	'bootstrap-select': 'libs/bootstrap-select',

	// Jquery plugin to add a ToTopBottom button
	'totop': 'libs/jquery-plugins/jquery-totop',

	// Jquery plugin to allow element sorting on jquery collections
	'sort': 'libs/jquery-plugins/jquery-sort',

	// requirejs ractive plugin
	'rv': 'libs/requirejs-plugins/rv',

	// requirejs i18n plugin
	'i18n': 'libs/requirejs-plugins/i18next',

	// The i18next library
	'i18next': 'libs/i18next.amd.withJQuery',

	// Builder needed for optimization process
	'i18next-builder': 'libs/i18next-builder',

	// used for optimization of the rv plugin
	'ractive': 'libs/ractive',

	// runtime version of ractive
	'ractivejsWithoutDefaults': 'libs/ractive.runtime',

	// return the Ractive module with the custom default options (to use in
	// modules)
	'ractivejs': 'libs/ractive.customDefaults',

	'ui': '../ui',
	'templates': '../ui/templates',
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
    i18next: {
	fallbackLng: "en-gb",
	fallbackOnNull: true,
	lowerCaseLng: true,
	resGetPath: "__lng__/__ns__.json",
	ns: "stratumProxy",
	supportedLngs: {
            'en-gb': ["stratumProxy"],
            'fr-fr': ["stratumProxy"]
        }
    },
    inlineI18next: true,
    stubModules: ["i18n", "i18next-builder", "rv", "ractive"]

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

// IE CustomEvent polyfill
(function () {
    'use strict';
    function CustomEvent (event, params) {
        params = params || { bubbles: false, cancelable: false, detail: undefined };
        var evt;
        try {
            evt = document.createEvent('CustomEvent');
            evt.initCustomEvent(event, params.bubbles, params.cancelable, params.detail);
        } catch (error) {
            // fallback for browsers that don't support createEvent('CustomEvent')
            evt = document.createEvent('Event');
            for (var param in params) {
                evt[param] = params[param];
            }
            evt.initEvent(event, params.bubbles, params.cancelable);
        }
        return evt;
    }

    if(typeof window.CustomEvent !== 'undefined') {
        CustomEvent.prototype = window.CustomEvent.prototype;
    }

    window.CustomEvent = CustomEvent;
})();