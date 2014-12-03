require([''])

/**
 * Retrieve the template with the given name and call the callback function when
 * retrieved. The callback function is called with the resulting template as
 * parameter.
 * 
 * If the template cannot be retrieved, the failedCallback is called with the
 * textStatus and the errorThrown parameters.
 * 
 * @param templateName
 * @param callback
 * @param errorCallback
 */
function getTemplateWithName(templateName, callback, errorCallback) {
    var templateUrl = '/ui/templates/' + templateName + ".handlebars";
    $.ajax({
	url : templateUrl,
	success : function(data) {
	    var template = Handlebars.compile(data);
	    if (callback != undefined) {
		callback(template);
	    }
	},
	error : function(jqXHR, textStatus, errorThrown) {
	    if (errorCallback != undefined) {
		errorCallback(textStatus, errorThrown);
	    }
	}
    });
}