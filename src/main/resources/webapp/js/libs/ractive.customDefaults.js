define(['ractivejsWithoutDefaults', 'i18next', 'moment', 'jquery'], function(Ractive, i18next, moment, $) {
    Ractive.defaults.append = true;

    var helpers = Ractive.defaults.data;

    helpers.translate = function(keyPath) {
	// get('locale') is here only to act as a trigger when the locale data value changes 
	// since Ractivejs reevaluates functions that depends on a reference that has
	// changed.
	var locale = this.get('locale');
	return i18next.t(keyPath);
    };

    helpers.formatDuration = function(duration) {
	// get('locale') is here only to act as a trigger when the locale data value changes 
	// since Ractivejs reevaluates functions that depends on a reference that has
	// changed.
	var locale = this.get('locale');
	return duration != 0 ? moment.duration(duration).humanize() : 0;
    }

    helpers.formatDateTime = function(timestamp) {
	// get('locale') is here only to act as a trigger when the locale data value changes 
	// since Ractivejs reevaluates functions that depends on a reference that has
	// changed.
	 var locale = this.get('locale');
	return moment(timestamp).format('llll');
    }

    var I18nRactive = Ractive.extend({
	onrender: function(event, newLocale) {
	    // Add a listener to update the locale for all I18nRactive instance
	    document.addEventListener('localeChanged', $.proxy(function(event) {
		this.set('locale', event.detail.newLocale);
	    }, this), false);
	}
    });

    return I18nRactive;
});