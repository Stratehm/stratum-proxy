define(['jquery'], function($) {

    /*
     * Define the page controller class
     */
    var PageController = function(pageName) {
	this.pageName = pageName;
    };

    PageController.prototype.hide = function() {
	this.getContainer().hide();
    };

    PageController.prototype.show = function() {
	this.getContainer().show();
    };

    PageController.prototype.load = function(mainContainer) {
	if (this.onLoad != undefined) {
	    this.onLoad(mainContainer);
	}
    };

    PageController.prototype.unload = function() {
	this.hide();
	if (this.onUnload != undefined) {
	    this.onUnload();
	}
	this.getContainer().remove();
    };

    PageController.prototype.refresh = function() {
	// Do nothing. Will be implemented by sub-classes
    };

    PageController.prototype.getContainer = function() {
	return $('#' + this.pageName);
    };

    return PageController;
});