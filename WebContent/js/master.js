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
		showMessage(messageClass, messageText);			
	}		
});

/**
 * Function to display a message to the user. We can call this from other javascript
 * functions or we can place special message divs in HTML to be picked out on page load
 * @param type The type of the message (the class name: error, warn, info or success)
 * @param message The message to display
 */
function showMessage(type, message) {
	// Create the element that will allow the user to close the message
	var closeMessage = $("<span class='exit'>X</span>");
	
	// Create a new DOM element to insert to display the message, and inject its classes and message
	var message = $("<div></div>").text(message).attr('class', type + " message").append(closeMessage);
	
	// When the close element is clicked, close the message and remove it from the DOM
	$(closeMessage).click(function() {
		$(message).slideUp(500, function(){
			$(message).remove();
		});
	});
	
	// Hide the message, then put it at the top of the page and slide it down, ya dig?
	$(message).hide().prependTo($('body')).slideDown();		
}