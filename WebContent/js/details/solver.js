var solverId;
$(document).ready(function(){
	initUI();
	attachButtonActions();
	
	$("#dialog-confirm-copy").css("display", "none");


	$('img').click(function(event){
		popUp($(this).attr('enlarge'));
	});
	
	$('.confirmation').on('click', function () {
        return confirm('Are you sure?');
    });
});

//	solverId = getParameterByName('id');





function initUI(){
	$('#downLink3').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});
	$('#fieldSites').expandable(true);
	$('#actions').expandable(true);
	
	
	// Setup datatable of configurations
	$('#tblSolverConfig').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',        
        "bPaginate": true,        
        "bSort": true        
    });

	
	
	$( "#dialog-confirm-delete" ).hide();
	$( "#dialog-warning").hide();
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
	
	//Warn if solver is uploaded without configuration
	var flag = window.location.search.split('flag=')[1]; //Check to see if flag was set
	if (flag == 'true') {
		$('#dialog-warning-txt').text('WARNING: No Configurations.');
		
		$('#dialog-warning').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-warning').dialog('close');
				}
			}
		});
	}
}

function attachButtonActions() {
	$("#downLink3").click(function(){
		$('#dialog-confirm-copy-txt').text('How would you like to download the solver?');		
		$('#dialog-confirm-copy').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'default': function() {
					log("default selected");
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large solvers.");
					token=Math.floor(Math.random()*100000000);
					window.location.href = starexecRoot+"secure/download?token=" + token + "&type=solver&id="+$("#solverId").attr("value");
					//$('#downLink3').attr('href', starexecRoot+"secure/download?token=" + token + "&type=solver&id=" + $("#solverId").attr("value"));
					destroyOnReturn(token);
				},
				'Re-upload': function() {
					log("upload selected");
					$(this).dialog("close");
					createDialog("Processing your download request, please wait. This will take some time for large solvers.");
					token=Math.floor(Math.random()*100000000);
					window.location.href = starexecRoot+"secure/download?token=" + token + "&type=reupload&id="+$("#solverId").attr("value");
					//$('#downLink3').attr('href', starexecRoot+"secure/download?token=" + token + "&type=reupload&id=" + $("#solverId").attr("value"));
					destroyOnReturn(token);
				},
				"cancel": function() {
					log('user canceled copy action');
					$(this).dialog("close");
				}
			}
		});
	});
}

function popUp(uri) {
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


