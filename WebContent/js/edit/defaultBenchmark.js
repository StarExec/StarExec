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
		"iDisplayLength": defaultPageSize,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/space/",
		"sServerMethod" : "POST",
		"fnServerData"	: fnPaginationHandler
	});
	
	$("#benchmarks").on("mousedown", "tr",function() {
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
					starexecRoot+"services/edit/space/" + "defaultBenchmark" + "/" + $("#cid").attr("value"),
					{val : selectedBench},
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");
						}

					},
					"json"
			);
		} else {
			showMessage('error',"Select a benchmark to proceed","5000");
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
