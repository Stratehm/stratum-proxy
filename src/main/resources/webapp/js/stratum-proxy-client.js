define(['jquery', 'ractivejs', 'rv!templates/mainContainer', 'i18n!locales', 'locales/localesConfig',
	'bootstrap', 'bootstrap-select', 'totop', 'highstock'], function($, Ractive, template, i18next,
	localesConfig) {

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

	initI18n();

	loadPageController('poolsPage');
    }

    function initContainer() {
	$('#loadingWrapper').empty();

	this.ractive = new Ractive({
	    el: $('body'),
	    template: template,
	    oncomplete: function() {
		$('body').i18n();
		initVersion();
	    }
	});
	

    }
    
    function initVersion() {
	$.ajax({
	    url: "proxy/misc/version",
	    dataType: "json",
	    type: "GET",
	    contentType: "application/json",
	    context: this,
	    success: function(data) {
		ractive.set(data);
	    },
	    error: function(request, textStatus, errorThrown) {
		ractive.set('proxyVersion', 'Unknown')
	    }
	});
    }

    function initI18n() {
	var localesSelect = $('#localeSelect').selectpicker();
	var currentLocale = i18next.lng() || 'en-gb';

	// Populate available locales
	if (localesConfig && localesConfig.locales) {
	    localesConfig.locales
		    .forEach(function(element, index, array) {
			var option = $(new Option(element.label, element.code));
			var src = "src='" + (element.iconData ? element.iconData : element.iconUrl) + "'";
			option.attr('data-content', "<img class='countryFlagThumbnail' " + src + ">"
				+ element.label);
			localesSelect.append(option);
		    });

	    localesSelect.val(currentLocale);
	    localesSelect.selectpicker('val', currentLocale);
	}

	// Refresh UI with updates locales and selected one
	localesSelect.selectpicker('render');
	localesSelect.selectpicker('refresh');

	// Add listener when changing locale
	localesSelect.change(function() {
	    var selectedLocale = localesSelect.find('option:selected').val();
	    setLocale(selectedLocale);
	});

	setLocale(currentLocale);
    }

    function setLocale(newLocale) {
	i18next.setLng(newLocale, {
	    fixLng: true
	}, function(lng) {
	    $('body').i18n();
	    fireLocaleChangedEvent();
	});
    }

    function fireLocaleChangedEvent() {
	var event = document.createEvent('Event');
	event.initEvent('localeChanged', false, false);
	document.dispatchEvent(event);
    }

    function initToTopScroller() {
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
	    linkClass: 'totopscroller-lnk'
	});

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

	document.addEventListener('localeChanged', function() {
	    setHighchartsLanguage();
	}, false);

	setHighchartsLanguage();

	function setHighchartsLanguage() {
	    Highcharts.setOptions({
		lang: {
		    months: i18next.t('charts.months', {
			returnObjectTrees: true
		    }),
		    shortMonths: i18next.t('charts.shortMonths', {
			returnObjectTrees: true
		    }),
		    thousandsSep: i18next.t('charts.thousandsSep'),
		    noData: i18next.t('charts.noData'),
		    numericSymbols: i18next.t('charts.numericSymbols', {
			returnObjectTrees: true
		    }),
		    weekdays: i18next.t('charts.weekDays', {
			returnObjectTrees: true
		    })
		}
	    });
	}
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
