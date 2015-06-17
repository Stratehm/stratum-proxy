define(['jquery', 'json'], function($) {

    function AuthenticationManager() {
	this.authenticationDetails = null;
	this.isAuthenticated = false;
    }

    AuthenticationManager.prototype.getAuthenticationDetails = function(callback) {
	if (this.authenticationDetails != null && callback) {
	    callback(this.authenticationDetails);
	} else {
	    var manager = this;
	    $.ajax({
		url: "proxy/misc/authentication/details",
		dataType: "json",
		type: "GET",
		contentType: "application/json",
		success: function(data) {
		    if (data != undefined) {
			manager.authenticationDetails = data;
			// If the authentication is not needed, consider that we
			// are authenticated.
			if (!data.authenticationNeededForWriteAccess) {
			    manager.isAuthenticated = true;
			    fireLoginSuccessEvent();
			}
			if (callback) {
			    callback(data);
			}
		    }
		}
	    });
	}
    }
    
    function fireLoginSuccessEvent() {
	var event = new CustomEvent('loginSuccess');
	document.dispatchEvent(event);
    }

    AuthenticationManager.prototype.authenticate = function(onSuccess, onFailure) {
	// Just send a request to a protected API method to trigger an
	// authentication
	var manager = this;
	$.ajax({
	    url: "proxy/log/level",
	    dataType: "json",
	    type: "GET",
	    contentType: "application/json",
	    success: function() {
		manager.isAuthenticated = true;
		if (onSuccess) {
		    fireLoginSuccessEvent();
		    onSuccess();
		}
	    },
	    error: function(jqXHR) {
		manager.isAuthenticated = false;
		if (onfailure) {
		    onFailure();
		}
	    }
	});
    }

    return new AuthenticationManager();
});
