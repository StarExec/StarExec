$(document).ready(function(){
<<<<<<< .mine
	$('#fieldSites').expandable(true);			
});

var flag = true;

function changeImage(obj){
	
	if (flag){
		var splitString = obj.src.split("&type=");
		obj.src = splitString[0] + ("&type=sorg");
		obj.width = 600;
	} else{
		var splitString = obj.src.split("&type=");
		obj.src = splitString[0] + ("&type=sthn");
		obj.width = 150;
	}
	flag = !flag;
}=======
	// Builds the user interface
	initUI();
	
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
}>>>>>>> .r9522
