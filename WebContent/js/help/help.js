$(document).ready(function(){	
	attachClickEvents();
	findReferringFile();
});

/**
 * Removes the "active" class from every topic link
 */

function removeActiveLinks() {
	$("#topicList a").each(function() {
		$(this).removeClass("active");
	});
}

/**
 * For every topic link, attaches an event that makes that topic get loaded into the topic
 * pane on the right
 */

function attachClickEvents() {
	$("#topicList a").click(function(event) {
		removeActiveLinks();
		$(this).addClass("active");
		//load the contents of the help file into the right hand side
		$.get( $(this).attr("href"), function( data ) {
				$( "#detailPanel" ).html( data );
		},"html").error(function(){
			showMessage('error',"Internal error retrieving help page",5000);
		});;
		return false; // this ensures that we don't actually follow the link
	})
}
/**
 * Tries to load the file that relates to the page the user came from. If no such file can be found,
 * then the main help page is loaded.
 */
function findReferringFile() {
	reference=$("#reference").attr("href");
	$("#topicList a").each(function() {
		if (reference==$(this).attr("href")) {
			$(this).trigger("click");
		}
	});
}