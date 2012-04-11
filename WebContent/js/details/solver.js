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
			primary: "ui-icon-arrowthick-1-n"
		}
    });	
}

function PopUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);

	imageTag.load(function(){
		
		var resizedHieght = 400;
			
		$('#popDialog').dialog({
			dialogClass: 'alert',
			modal: true,
			resizable: true,
			draggable: false,
			height: resizedHieght,
			width: 'auto'
		});
	});  
}