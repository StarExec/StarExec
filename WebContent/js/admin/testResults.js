var testTable;

$(document).ready(function(){
	initUI();
	setInterval(function() {
		testTable.fnReloadAjax();
		
	},5000);
});



function initUI(){
	
	extendDataTableFunctions();
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
