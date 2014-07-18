(function($) {
	$.fn.totopscroller = function(options) {

		var superThis = this;
		
		var defaults = {
			showToBottom : true,
			showToPrev : true,
			link : false,
			linkTarget : '_self',
			toTopHtml : '<a href="#"></a>',
			toBottomHtml : '<a href="#"></a>',
			toPrevHtml : '<a href="#"></a>',
			linkHtml : '<a href="#"></a>',
			toTopClass : 'totopscroller-top',
			toBottomClass : 'totopscroller-bottom',
			toPrevClass : 'totopscroller-prev',
			linkClass : 'totopscroller-lnk',
		};

		var settings = $.extend({}, defaults, options);

		var wrapper = this, b_top = null, b_bottom = null, b_link = null, b_wrapper = null;

		var init = function() {
			b_wrapper = $('<div></div>');
			if (settings.showToBottom) {
				b_bottom = $(settings.toBottomHtml);
				b_bottom.hide();
				b_bottom.addClass(settings.toBottomClass);
				b_bottom.appendTo(b_wrapper);
			}
			b_top = $(settings.toTopHtml);
			b_top.hide();
			b_top.addClass(settings.toTopClass);
			b_top.appendTo(wrapper);
			if (settings.link) {
				b_link = $(settings.linkHtml);
				b_link.attr("target", settings.linkTarget);
				b_link.attr("href", settings.link);
				b_link.addClass(settings.linkClass);
				b_link.appendTo(wrapper);
			}
			b_wrapper.appendTo(wrapper);

			b_top.click(function(e) {
				e.preventDefault();
				$('html, body').animate({
					scrollTop : 0
				}, {
					complete : function() {
						superThis.refresh();
					}
				});
			});
			if (settings.showToBottom) {
				b_bottom.click(function(e) {
					e.preventDefault();
					$('html, body').animate({
						scrollTop : $(document).height()
					}, {
						complete : function() {
							superThis.refresh();
						}
					});
				});
			}
		}

		this.refresh = function() {
			if ($(document).scrollTop() > 0) {
				b_top.fadeIn("fast");
			} else {
				b_top.fadeOut("fast");
			}

			if ($(window).scrollTop() + $(window).height() == $(document)
					.height()) {
				b_bottom.fadeOut("fast");
			} else {
				b_bottom.fadeIn("fast");
			}
		}

		$(window).scroll(function() {
			if ($('html, body').is(":animated"))
				return;
			superThis.refresh();

		});

		init();
		this.refresh();
		return this;
	};
}(jQuery));