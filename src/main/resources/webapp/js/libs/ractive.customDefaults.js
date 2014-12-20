define(['ractivejsWithoutDefaults', 'i18next'], function(Ractive, i18next) {
    Ractive.defaults.append = true;

    var helpers = Ractive.defaults.data;

    helpers.translate = function(keyPath) {
	return i18next.t(keyPath);
    };

    return Ractive;
});