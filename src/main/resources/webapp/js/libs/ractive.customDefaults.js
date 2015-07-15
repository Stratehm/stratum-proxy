define(['ractivejsWithoutDefaults', 'i18next', 'moment', 'jquery', 'd3'], function(Ractive, i18next, moment, $, d3) {
    Ractive.defaults.append = true;

    var helpers = Ractive.defaults.data;

    // Check if the browser supports the locale option to format numbers.
    var numberLocaleSupported = (function testSupporttoLocaleString() {
	var nombre = 0;
	try {
	    nombre.toLocaleString("i");
	} catch (e) {
	    return e.name === "RangeError";
	}
	return false;
    })();

    helpers.translate = function(keyPath) {
	// get('locale') is here only to act as a trigger when the locale data
	// value changes
	// since Ractivejs reevaluates functions that depends on a reference
	// that has
	// changed.
	var locale = this.get('locale') || i18next.lng() || 'en-gb';
	return i18next.t(keyPath);
    };

    helpers.formatDuration = function(duration) {
	// get('locale') is here only to act as a trigger when the locale data
	// value changes
	// since Ractivejs reevaluates functions that depends on a reference
	// that has
	// changed.
	var locale = this.get('locale') || i18next.lng() || 'en-gb';
	return duration != 0 ? moment.duration(duration).humanize() : 0;
    }

    helpers.formatDateTime = function(timestamp) {
	// get('locale') is here only to act as a trigger when the locale data
	// value changes
	// since Ractivejs reevaluates functions that depends on a reference
	// that has
	// changed.
	var locale = this.get('locale') || i18next.lng() || 'en-gb';
	return moment(timestamp).format('llll');
    }

    helpers.formatHashrate = function(hashrate) {
	// get('locale') is here only to act as a trigger when the locale data
	// value changes since Ractivejs reevaluates functions that depends on a
	// reference that has changed.
	var locale = this.get('locale') || i18next.lng() || 'en-gb';
	var prefix = d3.formatPrefix(hashrate, 3);
	var scale = prefix.scale(hashrate);
	
	// Format the prefix based on the locale.
	if (Number.toLocaleString) {
	    if (numberLocaleSupported) {
		scale = scale.toLocaleString(locale);
	    } else {
		scale = scale.toLocaleString();
	    }
	}
	return scale + ' ' + prefix.symbol + 'H/s';
    }
    
    helpers.formatDifficulty = function(difficulty) {
	// get('locale') is here only to act as a trigger when the locale data
	// value changes since Ractivejs reevaluates functions that depends on a
	// reference that has changed.
	var locale = this.get('locale') || i18next.lng() || 'en-gb';
	var result = difficulty;
	
	// Format the prefix based on the locale.
	if (Number.toLocaleString) {
	    if (numberLocaleSupported) {
		result = result.toLocaleString(locale);
	    } else {
		result = result.toLocaleString();
	    }
	}
	return result;
    }

    var I18nRactive = Ractive.extend({
	oninit: function(event, newLocale) {
	    // Add a listener to update the locale for all I18nRactive instance
	    document.addEventListener('localeChanged', $.proxy(function(event) {
		this.set('locale', event.detail.newLocale);
	    }, this), false);
	}
    });

    return I18nRactive;
});