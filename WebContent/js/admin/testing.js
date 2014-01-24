var testTable;

$(document).ready(function(){
	initUI();
	setInterval(function() {
		rows = $(testTable).children('tbody').children('tr.row_selected');
		if (rows.length==0) {
			testTable.fnReloadAjax();
		}
	},5000);
});



function initUI(){
	
	$("#runAll").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#runSelected").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	
	$("#runAll").click(function() {
		$.post(
				starexecRoot+"services/test/runAllTests",
				{},
				function(returnCode) {
					if (returnCode=="0") {
						showMessage("success","testing started succesfully",5000);
					} else {
						showMessage("error","There was an error while starting the testing",5000);
					}
				},
				"json"
		);
		unselectAllRows(testTable);
	});
	
	$("#runSelected").click(function() {
		nameArray=getSelectedRows(testTable);
		$.post(
			starexecRoot+"services/test/runTests",
			{testNames : nameArray},
			function(returnCode) {
				if (returnCode=="0") {
					showMessage("success","testing started succesfully",5000);
				} else {
					showMessage("error","There was an error while starting the testing",5000);
				}
			},
			"json"
		);
		unselectAllRows(testTable);
	});
	
	extendDataTableFunctions();
	testTable=$('#tableTests').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true,
        "sAjaxSource"	: starexecRoot+"services/tests/pagination",
        "sServerMethod" : "POST",
        "fnServerData" : fnPaginationHandler
    });

	
	$("#tableTests").delegate("tr", "click", function() {
		$(this).toggleClass("row_selected");
	});

}

function extendDataTableFunctions() {	
	$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource ) {
	    if ( typeof sNewSource != 'undefined' )
	    oSettings.sAjaxSource = sNewSource;
	     
	    this.fnClearTable( this );
	    this.oApi._fnProcessingDisplay( oSettings, true );
	    var that = this;
	     
	    $.getJSON( oSettings.sAjaxSource, null, function(json) {
	    /* Got the data - add it to the table */
	    for ( var i=0 ; i<json.aaData.length ; i++ ) {
	    that.oApi._fnAddData( oSettings, json.aaData[i] );
	    }
	     
	    oSettings.aiDisplay = oSettings.aiDisplayMaster.slice();
	    that.fnDraw( that );
	    that.oApi._fnProcessingDisplay( oSettings, false );
	    });
	}
}

/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 * 
 * @param dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
 * @author Todd Elvers
 */
function getSelectedRows(dataTable){
	var nameArray = new Array();
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		nameArray.push($(this).children('td:first').html());
		alert($(this).children("td:first").html());
	});
	return nameArray;
}

function unselectAllRows(dataTable) {
	$(dataTable).find("tr").each(function() {
		$(this).removeClass("row_selected");
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	$.get(  
			sSource,
			function(nextDataTablePage){
				
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view job pairs for this job", 5000);
						break;
					default:
						fnCallback(nextDataTablePage);						
						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}
