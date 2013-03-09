var debugMode = false; //console.log statements are turned off by default

/**
 * Contains javascript relevant to all pages within starexec 
 */

// When the document is ready to be executed on
$(document).ready(function(){
		
	// If the JSP contains a single message to display to the user...
	if ($(".message").length == 1){
				
		// Extract the text from the message element
		var messageText = $(".message").text();
		
		// Determine which class it is (error, warn, info, success)
		// This class MUST come first in the class list if specified in HTML
		var messageClass = $('.message').attr('class').split(' ')[0];
		
		// Remove the old message element from the DOM
		$(".message").remove();
		
		// Show the message to the user (we do this programatically so we can re-use code)
		showMessage(messageClass, messageText, 10000);			
	}
		
	// Setup navigation submenus
	$("#pageHeader nav ul li").hover(function() {
		// When we hover over a menu item...
		// Find their submenu and slide it down
		$(this).find("ul.subnav").stop(true,true);
		$(this).find("ul.subnav").slideDown('fast').show();
		
		
		
		// Then attach a hover out event  
		$(this).hover(function() {}, 
			function(){  
				// When I'm hovered out of, slide up my submenu 
				
				$(this).find("ul.subnav").slideUp('fast'); 
				
	           	 
	        });
		}, function () {});	 

	// Extend jquery functions here
	$.fn.extend({
		toHTMLString: function() {
			return $("<div>").append(this.clone()).html();
		},
		expandable: function(closed, callback) {
			// Makes a fieldset expandable
			$(this).each(function() { 	
				var legend = $(this).children('legend:first');				
				$(legend).css('cursor', 'pointer');
				$(legend).siblings().wrapAll('<div class="expdContainer" />');
				
				if(closed) {
					$(legend).data('open', false);
					$(legend).append('<span> (+)</span>');	
					$(legend).siblings().hide();		
				} else {
					$(legend).data('open', true);
					$(legend).append('<span> (-)</span>');		
				}
					
				$(legend).click(function() {					
					var isOpen = $(this).data('open');				
					$(this).children('span:last-child').text(isOpen ? ' (+)' : ' (-)');			
					$(this).data('open', !isOpen);				
					$(this).siblings().slideToggle('fast');
					
					if(!isOpen && $.isFunction(callback)) {
						callback.call(this);
					}
				});
	       	}); 
		    return $(this); 				
		}
	}); 
});

/**
 * Prints a message to the Chrome javascript console if debugging is enabled
 * 
 * @param message the message to print to Chrome's javascript console
 */
function log(message){
	if(true == debugMode){
		console.log(message);
	}
}

/**
 * Function to display a message to the user. We can call this from other javascript
 * functions or we can place special message divs in HTML to be picked out on page load
 * @param type The type of the message (the class name: error, warn, info or success)
 * @param message The message to display
 * @param duration How long (in milliseconds) before the notification auto-closes itself. Anything <= 0 disables auto-hide
 */
function showMessage(type, message, duration) {
	// Create the element that will allow the user to close the message
	var closeMessage = $("<span class='exit'>X</span>");
	var messageSpan = $("<div></div>").html(message);
	
	// Create a new DOM element to insert to display the message, and inject its classes and message
	var msg = $("<div><img src='/starexec/images/icons/exclaim.png' /></div>").attr('class', type + " message");
	$(msg).append(messageSpan);
	$(msg).append(closeMessage);
	
	// When the close element is clicked, close the message and remove it from the DOM
	$(closeMessage).click(function() {
		// If the X is clicked before the message's duration runs out,
		// stop the duration timer and close the element immediately
		clearTimeout($(msg).stop().data('timer'));
		$(msg).slideUp(500, function(){
			$(msg).remove();
		});
	});
	
	// Hide the message, then put it at the top of the page and slide it down, ya dig?
	$(msg).hide().prependTo($('body')).slideDown(500, function(){
		if(duration > 0) {
			// After the specified duration, slide it up and remove it from the DOM
		    $.data(this, 'timer', setTimeout(function() { 
		    	$(msg).slideUp(500, function(){
					$(msg).remove();
				});
		    }, duration));
		}
	});		
}

/**
 * Invalidates the user's session on the server and refreshes
 * the current page to send the user back to the login page.
 */
function logout() {
	$.post(  
	    "/starexec/services/session/logout",  
	    function(returnData){  
	        if(returnData == 0) {
	        	window.location.reload(true);
	        }  
	     },  
	     "json"  
	).error(function(){
		alert("There was an error logging you out. Please try refreshing this page or restarting your browser");
	});
}


/**
 * Extracts parameters from the URL by name
 * 
 * @param name the name of the variable to extract from the URL
 */
function getParameterByName(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
}

/**
 * Converts text to html encoded text 
 * @param s - text to encode
 * @author Vivek Sardeshmukh
 */
function HtmlEncode(s) {
	var el = document.createElement("div");
	el.innerText = el.textContent = s;
	s = el.innerHTML;
	return s;
}

/**
 * Returns the regular expression used to validate primitive names
 */
function getPrimNameRegex(){
	return "^[\\w\\-\\.\\s]+$";
}

/**
 * Returns the regular expression used to validate primitive descriptions
 */
function getPrimDescRegex(){
	return "[\\w\\s\\-\\[\\]\\\\~!@#$%^&*():;`,{}'\"/|=+?.]+$";
}

/**
 * Returns the regular expression used to validate user names
 */
function getUserNameRegex(){
	return "^[a-zA-Z\\-'\\s]+$";
}
