var testTable;

$(document).ready(function(){
	initUI();
	setInterval(function() {
		testTable.api().ajax.reload(null,false);
		
	},5000);
});



function initUI(){
	
	testTable=$('#tableTests').dataTable( {
        "sDom"			: getDataTablesDom(),
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bSort": true,
        "bPaginate": true,
        "sAjaxSource"	: starexecRoot+"services/testResults/pagination/"+$("#sequenceName").attr("value"),
        "sServerMethod" : "POST",
        "fnServerData" : fnPaginationHandler
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
