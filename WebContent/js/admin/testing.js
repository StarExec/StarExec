var testTable;

$(document).ready(function(){
	initUI();
	setInterval(function() {
		rows = $(testTable).children('tbody').children('tr.row_selected');
		if (rows.length==0) {
			testTable.api().ajax.reload(null,false);
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
	$("#runStress").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	
	$("#runAll").click(function() {
		$.post(
				starexecRoot+"services/test/runAllTests",
				{},
				function(returnCode) {
					s=parseReturnCode(returnCode);

					
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
				s=parseReturnCode(returnCode);

				
			},
			"json"
		);
		unselectAllRows(testTable);
	});
	$("#runStress").click(function() {
		window.open(starexecRoot+"secure/admin/stressTest.jsp");
	});
	
	testTable=$('#tableTests').dataTable( {
        "sDom"			: getDataTablesDom(),
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bSort": true,
        "bPaginate": true,
        "sAjaxSource"	: starexecRoot+"services/tests/pagination",
        "sServerMethod" : "POST",
        "fnServerData" : fnPaginationHandler
    });

	
	$("#tableTests").on( "click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

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
		nameArray.push($(this).find('a:first').attr("name"));
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
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					fnCallback(nextDataTablePage);						
				}

			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}
