define(['jquery', 'ractive', 'rv!templates/mainContainer', 'bootstrap', 'totop', 'highstock'], function($,
	Ractive, template) {

    // Store the current controller
    var currentController = null;

    // Stores the instances of page controllers.
    var pageControllers = {};

    /**
     * Launch the client.
     */
    function launchClient() {
	initContainer();

	initToTopScroller();

	initHighcharts();

	initNavBarHandlers();

	loadPageController('poolsPage');
    }

    function initContainer() {
	$('#loadingDiv').empty();

	var ractive = new Ractive({
	    el: $('body'),
	    template: template
	});

	this.toTopBottomScroller = $('#totopscroller').totopscroller({
	    showToBottom: true,
	    link: false,
	    linkTarget: '_self',
	    toTopHtml: '<a href="#"></a>',
	    toBottomHtml: '<a href="#"></a>',
	    toPrevHtml: '<a href="#"></a>',
	    linkHtml: '<a href="#"></a>',
	    toTopClass: 'totopscroller-top',
	    toBottomClass: 'totopscroller-bottom',
	    toPrevClass: 'totopscroller-prev',
	    linkClass: 'totopscroller-lnk',
	});
    }

    function initToTopScroller() {
	$(document).ajaxComplete(function() {
	    refreshToTopScroller();
	});
    }

    /**
     * Refresh the to Top/Bottom scroller
     */
    function refreshToTopScroller() {
	this.toTopBottomScroller.refresh();
    }

    /**
     * Initialize highcharts global options
     */
    function initHighcharts() {
	Highcharts.setOptions({
	    global: {
		useUTC: false
	    }
	});
    }

    /**
     * Initialize the navigation bar handler
     */
    function initNavBarHandlers() {
	$('#navbarUl a').click(function(e) {
	    onNavbarButtonSelection(this);
	});
    }

    /**
     * Process the navbar button selection event
     */
    function onNavbarButtonSelection(navbarButton) {
	var jqueryButton = $(navbarButton);

	// Remove the active of all other buttons
	jqueryButton.parent().parent().children().removeClass();
	// Then set the active button
	jqueryButton.parent().addClass('active');

	// Then show the selected page.
	loadPageController(jqueryButton.attr('page-name'));

    }

    /**
     * Load the controller for the given page name.
     */
    function loadPageController(pageName) {
	getPageController(pageName, function(pageController) {
	    if (currentController != undefined) {
		currentController.unload();
	    }

	    currentController = pageController;
	    currentController.load($('#pageContainer'));
	});
    }

    /**
     * Call the onGot callback once the pageController for the given page name
     * is available. The onGot callback is called with the pageController
     * instance as parameter.
     */
    function getPageController(pageName, onGot) {
	if (pageName in pageControllers) {
	    onGot(pageControllers[pageName]);
	} else {
	    require(['controllers/' + pageName], function(Controller) {
		if (currentController != undefined) {
		    currentController.unload();
		}

		var controller = new Controller(pageName);

		pageControllers[pageName] = controller;

		onGot(controller);
	    });
	}
    }

    return {
	launchClient: launchClient
    };
});
