var levelTable;

$(document).ready(function(){
	initUI();

});



function initUI(){
	
	$("#applyAll").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#applyToClass").button({
		icons: {
			primary: "ui-icon-check"
		}
	});

	
	$("#applyAll").click(function() {
		value=getSelectedRow(levelTable);
		$.post(
				starexecRoot+"services/logging/"+value[0],
				{},
				function(returnCode) {
					parseReturnCode(returnCode);
					
				},
				"json"
		);
	});
	
	$("#applyToClass").click(function() {
		value=getSelectedRow(levelTable);
		$.post(
			starexecRoot+"services/logging/"+value[0]+"/"+$("#className").val(),
			{},
			function(returnCode) {
				parseReturnCode(returnCode);

			},
			"json"
		);
	});
	
	
	levelTable=$('#tableLevels').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bSort": false,
        "bPaginate": true
    });

	
	$("#tableLevels").on( "click", "tr", function() {
		if (!$(this).hasClass("row_selected")) {
			unselectAllRows(levelTable);
		}
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
function getSelectedRow(dataTable){
	var valArray = new Array();
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		valArray.push($(this).attr("value"));
	});
	return valArray;
}

function unselectAllRows(dataTable) {
	$(dataTable).find("tr").each(function() {
		$(this).removeClass("row_selected");
	});
}


