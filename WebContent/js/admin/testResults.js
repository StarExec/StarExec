var testTable;

$(document).ready(function(){
	initUI();
	setInterval(function() {
		testTable.api().ajax.reload(null,false);
		
	},5000);
});



function initUI(){
	
	testTable=$('#tableTests').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
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
