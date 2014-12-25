define(['jquery', 'ractivejs', 'rv!templates/addPoolPopup', 'i18n!locales', 'bootstrap', 'json'], function($,
	Ractive, template, i18next) {

    var AddPoolPopup = function(parentController, parentElement) {
	this.parentController = parentController;

	this.ractive = new Ractive({
	    el: parentElement ? parentElement : $('body'),
	    template: template,
	    data: {
		isEnabled: true,
		workerNameSeparator: '.'
	    }
	});

	this.popup = $('#addPoolModal').modal({
	    keyboard: true,
	    backdrop: 'static'
	});
	this.popup.find('.validateButton').off('click').click($.proxy(validate, this));

	this.popup.i18n();

	this.popup.on('hidden.bs.modal', $.proxy(function() {
	    this.popup.remove();
	}, this));
    }

    function validate() {
	var thisController = this;

	$.ajax({
	    url: '/proxy/pool/add',
	    dataType: "json",
	    type: "POST",
	    data: JSON.stringify(thisController.ractive.get()),
	    contentType: "application/json",
	    context: thisController,
	    success: onSuccess,
	    error: onError
	});
    }

    function onSuccess(data) {
	if (data.status == 'Failed') {
	    window.alert('Failed to add the pool. Message: ' + data.message);
	} else {
	    if (data.status == 'PartiallyDone') {
		window.alert('Pool added but not started. Message: ' + data.message);
	    }

	    if (this.parentController != undefined && this.parentController.refresh != undefined) {
		this.parentController.refresh();
	    }
	}
	this.popup.modal('hide');
    }

    function onError(request, textStatus, errorThrown) {
	var jsonObject = JSON.parse(request.responseText);
	window.alert('Failed to add the pool. Status: ' + textStatus + ', error: ' + errorThrown
		+ ', message: ' + jsonObject.message);
	this.popup.modal('hide');
    }

    return AddPoolPopup;

});