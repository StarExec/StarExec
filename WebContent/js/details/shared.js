$(document).ready(function(){
	// Hide loading images by default
	$('legend img').hide();
	
	$('.popoutLink').button({
		icons: {
			secondary: "ui-icon-newwin"
    }});
	
	$('#downLink').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});

	$('#anonymousLink').button({
		icons: {
			// TODO change to something more appropriate
			secondary: "ui-icon-arrowthick-1-s"
		}
	});
	
	$('#editLink').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$('#returnLink, #returnLinkMargin').button({
		icons: {
			secondary: "ui-icon-arrowreturnthick-1-w"
		}});
	
	$('img').click(function(event){
		PopUp($(this).attr('enlarge'));
	});
	
});

function PopUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
		$('#popDialog').dialog({
			dialogClass: "popup",
			modal: true,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto'
		});
	});  
}
