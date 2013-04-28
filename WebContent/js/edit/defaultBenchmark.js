$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	//initialize benchmark table
	benchTable = $('#benchmarks').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: "/starexec/services/space/",
		"sServerMethod" : "POST",
		"fnServerData"	: fnPaginationHandler
	});
	
	$("#benchmarks").delegate("tr","mousedown",function() {
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
		window.location=href="/starexec/secure/edit/community.jsp?cid="+$("#cid").attr("value");
	});
	
}



/**
 * Returns the id of the selected benchmark or -1 if there is not one.
 * @author Eric Burns
 */
function benchSelected() {
	row=$("#benchmarks").find(".row_selected");
	if (row.length==0) { //means no benchmark is selected
		return -1;
	}
	input=row.find("input");
	id=input.attr("value");
	return id;
}

/**
 * Validates that a user has selected a benchmark when update is clicked
 */
function attachFormValidation(){
	
	$("#update").click(function(){
		var selectedBench = benchSelected();
		
		if(selectedBench>=0){
			createDialog("Updating default benchmark, please wait");
			$.post(
					"/starexec/services/edit/space/" + "defaultBenchmark" + "/" + $("#cid").attr("value"),
					{val : selectedBench},
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/edit/community.jsp?cid=' + $("#cid").attr("value");
								break;
							case 1:
								showMessage('error', "there was an error entering the updated information into the database", 5000);
								destroyDialog();
								break;
							case 2:
								showMessage('error', "only the leader of the community containing this processor can update it", 5000);
								destroyDialog();
								break;
							case 3:
								showMessage('error', "invalid parameters; please ensure you fill out all of the processor file's fields", 5000);
								destroyDialog();
								break;
						}
					},
					"json"
			);
		} else {
			showMessage("Select a benchmark to proceed","warn","5000");
		}
	});
}



function unselectAll() {
	$("#benchmarks").find("tr").removeClass("row_selected");
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
			sSource + idOfSelectedSpace + "/" + "benchmarks" + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
//					showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					//updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);

				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				break;
				}
			},  
			"json"
	).error(function(){
		
		window.location.reload(true);
	});
}
