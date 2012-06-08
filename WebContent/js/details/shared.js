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
			width: 'auto',
		});
	});  
}