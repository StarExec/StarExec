var primType;
$(document).ready(function(){
	primType=$("#primType").attr("value");

	initUI();
	attachFormValidation();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	//initialize primitive table
	primTable = $('#prims').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"fnServerData"	: fnPaginationHandler
	});
	
	$("#prims").on("mousedown", "tr",function() {
		if ($(this).hasClass("row_selected")) {
			$(this).removeClass("row_selected");
		} else {
			unselectAll();
			$(this).addClass("row_selected");
		}
	});
	
	// Attach icons
	$('#cancel').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	
	
	$("#cancel").click(function() {
		window.location=href=starexecRoot+"secure/edit/community.jsp?cid="+$("#cid").attr("value");
	});
	
}



/**
 * Returns the id of the selected primitive or -1 if there is not one.
 * @author Eric Burns
 */
function primSelected() {
	row=$("#prims").find(".row_selected");
	if (row.length==0) { //means no primitive is selected
		return -1;
	}
	input=row.find("input");
	id=input.attr("value");
	return id;
}

/**
 * Validates that a user has selected a primitive when update is clicked
 */
function attachFormValidation(){
	
	$("#update").click(function(){
		var selectedPrim = primSelected();
		
		if(selectedPrim>=0){
			createDialog("Updating default "+primType+", please wait");
			$.post(
					starexecRoot+"services/edit/defaultSettings/" + "default"+primType + "/" + $("#settingId").attr("value"),
					{val : selectedPrim},
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");
						}

					},
					"json"
			);
		} else {
			showMessage('error',"Select a "+primType+" to proceed","5000");
		}
	});
}



function unselectAll() {
	$("#prims").find("tr").removeClass("row_selected");
}

/**
 * Handles querying for pages in a given DataTable object
 * 
 * @param sSource the "sAjaxSource" of the calling table
 * @param aoData the parameters of the DataTable object to send to the server
 * @param fnCallback the function that actually maps the returned page to the DataTable object
 * @author Todd Elvers
 */
function fnPaginationHandler(sSource, aoData, fnCallback) {
	// Extract the id of the currently selected space from the DOM
	var idOfSelectedSpace = $('#cid').attr("value");
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + idOfSelectedSpace + "/" + primType+"s" + "/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},  
			"json"
	).error(function(){
		
		window.location.reload(true);
	});
}
