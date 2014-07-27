var typeTable;

$(document).ready(function(){
	initUI();
});

function initUI(){
	
	$("#clearAll").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#clearSelected").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	
	$("#clearStatsCache").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	
	
	$("#clearAll").click(function() {
		$.post(
				starexecRoot+"services/cache/clearAll",
				{},
				function(returnCode) {
					parseReturnCode(returnCode);
					
				},
				"json"
		);
		unselectAllRows(typeTable);
		
	});
	
	$("#clearSelected").click(function() {
		typeArray=getSelectedTypes(typeTable);
		$.post(
			starexecRoot+"services/cache/clearTypes",
			{selectedTypes : typeArray},
			function(returnCode) {
				parseReturnCode(returnCode);

			},
			"json"
		);
		unselectAllRows(typeTable);
	});
	
	$("#clearStatsCache").click(function() {
		$.post(
			starexecRoot+"services/cache/clearStats",
			{},
			function(returnCode) {
				parseReturnCode(returnCode);

			},
			"json"
		);
		unselectAllRows(typeTable);
	});
	
	
	typeTable=$('#tableTypes').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bSort": true,
        "bPaginate": true
    });

	
	$("#tableTypes").on( "click", "tr", function() {
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
function getSelectedTypes(dataTable){
	var typeArray = new Array();
	
	$(dataTable).children("tbody").children("tr.row_selected").find("span.cacheType").each(function() {
			typeArray.push($(this).attr("value"));
	});
	return typeArray;
}

function unselectAllRows(dataTable) {
	$(dataTable).find("tr").each(function() {
		$(this).removeClass("row_selected");
	});
}

