$(document).ready(function(){	
	
	$(".subject a").each(function(){
		url=makeFullURL($(this).attr("href"));
		$(this).attr("href",url);
	});
	attachClickEvents();
	findReferringFile();
});

//adds the help page to the url
function setURL(i) {
	window.history.replaceState("current page", "",makeFullURL(i));
}


//gets the current URL with no parameters
function getBaseURL() {
	current=window.location.pathname;
	return current.substring(0,current.indexOf("?"));
}


//given a reference, creates a link to the help.jsp page that includes the reference as a URL parameter
function makeFullURL(ref) {
	return getBaseURL()+"?ref="+ref;
}

//gets the reference link from the current URL 
function getRef(url) {
	return url.substring(url.indexOf("?ref=")+5);
}

/**
 * Removes the "active" class from every topic link
 */

function removeActiveLinks() {
	$("#topicList a").each(function() {
		$(this).removeClass("active");
	});
}
/**
 * Returns null if the content is not a stub
 */
function getStubURL(string) {
	if (string.indexOf("--STUB:")==0) {
		string=string.replace("--STUB:","").trim();
		string=starexecRoot +string;
		return string;
	} 
	return null;
}

function getHTML(URL) {
	//load the contents of the help file into the right hand side
	$.get( URL, function( data ) {
			stubURL=getStubURL(data);
			if (stubURL!=null) {
				selectMatchingReference(stubURL);
			} else {
				//support linking to other parts of Starexec in help files
				data=data.replace(/\$\{starexecRoot\}\//g,starexecRoot);
				$( "#detailPanel" ).html( data );
				setURL(URL);
			}
			
	},"html").error(function(){
		showMessage('error',"Internal error retrieving help page",5000);
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
		getHTML(getRef($(this).attr("href")));
		return false; // this ensures that we don't actually follow the link
	});
}
function selectMatchingReference(reference) {
	$("#topicList a").each(function() {
		if (reference==$(this).attr("href")) {
			$(this).trigger("click");
		}
	});
}

/**
 * Tries to load the file that relates to the page the user came from. If no such file can be found,
 * then the main help page is loaded.
 */
function findReferringFile() {
	reference=makeFullURL($("#reference").attr("href"));
	selectMatchingReference(reference);
}