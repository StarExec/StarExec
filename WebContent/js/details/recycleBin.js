var solverTable;
var jobTable;
$(document).ready(function(){
	$("fieldset").expandable(false);

	$('#clearSolvers').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$('#clearBenchmarks').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$('#restoreSolvers').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$('#restoreBenchmarks').button({
		icons: {
			secondary: "ui-icon-pencil"
	}});
	
	$('#clearBenchmarks').click(function(){
		createDialog("Clearing your recycled benchmarks, please wait. This will take some time for large numbers of benchmarks.");
		$.post(  
				starexecRoot +"services/deleterecycled/benchmarks",
				function(nextDataTablePage){
					destroyDialog();
					switch(nextDataTablePage){
						case 1:
							showMessage('error', "Internal error deleting benchmarks", 5000);
							break;
						default:
							benchTable.fnDraw(false);
	 						break;
					}
				},  
				"json"
		).error(function(){
			showMessage('error',"Internal error removing benchmarks",5000);
		});
	});
	
	$('#clearSolvers').click(function(){
		createDialog("Clearing your recycled solvers, please wait. This will take some time for large numbers of solvers.");
		$.post(  
				starexecRoot +"services/deleterecycled/solvers",
				function(nextDataTablePage){
					destroyDialog();
					switch(nextDataTablePage){
						case 1:
							showMessage('error', "Internal error deleting solvers", 5000);
							break;
						default:
							solverTable.fnDraw(false);
	 						break;
					}
				},  
				"json"
		).error(function(){
			showMessage('error',"Internal error removing solvers",5000);
		});
	});
	
	$('#restoreSolvers').click(function(){
		createDialog("Restoring your recycled solvers, please wait. This will take some time for large numbers of solvers.");
		$.post(  
				starexecRoot +"services/restorerecycled/solvers",
				function(nextDataTablePage){
					destroyDialog();
					switch(nextDataTablePage){
						case 1:
							showMessage('error', "Internal error restoring solvers", 5000);
							break;
						default:
							solverTable.fnDraw(false);
	 						break;
					}
				},  
				"json"
		).error(function(){
			showMessage('error',"Internal error removing solvers",5000);
		});
	});
	
	$('#restoreBenchmarks').click(function(){
		createDialog("Restoring your recycled benchmarks, please wait. This will take some time for large numbers of benchmarks.");
		$.post(  
				starexecRoot +"services/restorerecycled/benchmarks",
				function(nextDataTablePage){
					destroyDialog();
					switch(nextDataTablePage){
						case 1:
							showMessage('error', "Internal error restoring benchmarks", 5000);
							break;
						default:
							benchmarkTable.fnDraw(false);
	 						break;
					}
				},  
				"json"
		).error(function(){
			showMessage('error',"Internal error removing benchmarks",5000);
		});
	});
	

	//Initiate solver table
	solverTable = $('#rsolvers').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnRecycledPaginationHandler
    });
   
	//Initiate benchmark table
	benchTable = $('#rbenchmarks').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/users/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnRecycledPaginationHandler
    });
	
	
	
});


function fnRecycledPaginationHandler(sSource, aoData, fnCallback) {
	
	var tableName = $(this).attr('id');
	var usrId = $(this).attr("uid");
	
	$.post(  
			sSource + usrId + "/" + tableName + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view primitives for this user", 5000);
						break;
					default:
						
						updateFieldsetCount(tableName, nextDataTablePage.iTotalRecords);
 						fnCallback(nextDataTablePage);
 					
 						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating table",5000);
	});
}



/**
 * Helper function for fnPaginationHandler; since the proper fieldset to update
 * cannot be reliably found via jQuery DOM navigation from fnPaginationHandler,
 * this method provides manually updates the appropriate fieldset to the new value
 * 
 * @param tableName the name of the table whose fieldset we want to update (not in jQuery id format)
 * @param primCount the new value to update the fieldset with
 * @author Todd Elvers
 */
function updateFieldsetCount(tableName, value){
	switch(tableName[0]){
	case 'r':
		if ('s'==tableName[1]) {
			$("#recycledSolverExpd").children('span:first-child').text(value);
		} else {
			$("#recycledBenchExpd").children('span:first-child').text(value);
		}
	}
}


