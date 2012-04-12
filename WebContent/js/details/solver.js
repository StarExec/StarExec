$(document).ready(function(){
	
	$('#fieldSites').expandable(true);
	initUI();

	$('img').click(function(event){
		event.preventDefault();
		PopUp($(this).attr('enlarge'));
	});      
});

function initUI(){
	$('#fieldSites').expandable(true);		
	
	// Setup datatable of configurations
	$('#tblSolverConfig').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',        
        "bPaginate": true,        
        "bSort": true        
    });
	
	// Setup button icons
	$('#uploadConfig, #uploadConfigMargin').button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
    });
	
	$('#uploadPicture').button({
		icons: {
			primary: "ui-icon-gear"
		}
    });
	
	$(".close-image").click(function() {
	    $(this).parent().hide();
	});
	
	$('#popImage').css('height','400px');
}

function PopUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
		$('#popDialog').dialog({
			dialogClass: 'noTitle',
			modal: false,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto',
			maxWidth: 500,
			mxHeight: 400
		});
	});  
}