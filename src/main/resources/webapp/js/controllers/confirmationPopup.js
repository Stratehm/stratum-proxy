define(['jquery', 'ractivejs', 'rv!templates/confirmationPopup', 'i18n!locales'], function($, Ractive,
	template, i18next) {

    var defaultOptions = {
	displayYesButton: true,
	displayNoButton: true,
	displayCancelButton: true,
	targetElement: $('body'),
	title: 'title',
	message: 'message',
	yesCallback: undefined,
	noCallback: undefined,
	cancelCallback: undefined,
	callbackContext: undefined,
	yesResultValue: 'yes',
	noResultValue: 'no',
	cancelResultValue: 'cancel',
	autoCloseOnAction: true
    }

    var ConfirmationPopup = function(options) {
	var confirmationPopupId = ConfirmationPopup.nextConfirmationPopupId++;
	var opts = $.extend({}, defaultOptions, options);

	this.ractive = new Ractive({
	    el: opts.targetElement,
	    template: template,
	    data: {
		confirmationPopupId: confirmationPopupId,
		title: i18next.t(opts.title),
		message: i18next.t(opts.message)
	    }
	});

	this.popup = $('#confirmationModal-' + confirmationPopupId).modal({
	    keyboard: true,
	    backdrop: true
	});

	this.yesButton = this.popup.find('.yesButton');
	this.noButton = this.popup.find('.noButton');
	this.cancelButton = this.popup.find('.cancelButton')

	if (!opts.displayYesButton) {
	    this.yesButton.hide();
	}

	if (!opts.displayNoButton) {
	    this.noButton.hide();
	}

	if (!opts.displayCancelButton) {
	    this.cancelButton.hide();
	}

	this.yesButton.click($.proxy(function() {
	    this.result = opts.yesResultValue;
	    if (opts.yesCallback) {
		opts.yesCallback.call(opts.callbackContext ? opts.callbackContext : this, this.result);
	    }
	    if (opts.autoCloseOnAction) {
		this.hide();
	    }
	}, this));

	this.noButton.click($.proxy(function() {
	    this.result = opts.noResultValue;
	    if (opts.noCallback) {
		opts.noCallback.call(opts.callbackContext ? opts.callbackContext : this, this.result);
	    }
	    if (opts.autoCloseOnAction) {
		this.hide();
	    }
	}, this));

	this.popup.find('.cancelButton, .close').click($.proxy(function() {
	    if (opts.cancelCallback) {
		opts.cancelCallback.call(opts.callbackContext ? opts.callbackContext : this, this.result);
	    }
	    if (opts.autoCloseOnAction) {
		this.hide();
	    }
	}, this));

	this.result = opts.cancelResultValue;

	this.popup.on('hidden.bs.modal', $.proxy(function() {
	    this.popup.remove();
	}, this));

    }

    ConfirmationPopup.nextConfirmationPopupId = 0;

    ConfirmationPopup.prototype.hide = function() {
	this.popup.modal('hide');
    }

    ConfirmationPopup.prototype.getResult = function() {
	return this.result;
    }

    return ConfirmationPopup;
});