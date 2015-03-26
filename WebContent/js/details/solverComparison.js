var comparisonTable;
var jobId;
var spaceId;
var c1;
var c2;
var useWallclock=true;
$(document).ready(function(){
	jobId = getParameterByName('id');
	spaceId=getParameterByName('sid');
	c1=getParameterByName("c1");
	c2=getParameterByName("c2");
	initUI();
	initDataTables();
	setTimeButtonText();	
});

/**
 * Initializes the user-interface
 */
function initUI(){
	$(".changeTime").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	
	});
	
	$(".changeTime").click(function() {
		useWallclock=!useWallclock;
		setTimeButtonText();
		comparisonTable.fnDraw(false);
	});
}

/**
 * Initializes the DataTable objects
 */
function initDataTables(){
	extendDataTableFunctions();

	comparisonTable=$('#comparisonTable').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/jobs/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
	
	// Change the filter so that it only queries the server when the user stops typing
	$('#comparisonTbl').dataTable().fnFilterOnDoneTyping();
	
}

/**
 * Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
 */
function extendDataTableFunctions(){
	// Changes the filter so that it only queries when the user is done typing
	jQuery.fn.dataTableExt.oApi.fnFilterOnDoneTyping = function (oSettings) {
	    var _that = this;
	    this.each(function (i) {
	        $.fn.dataTableExt.iApiIndex = i;
	        var anControl = $('input', _that.fnSettings().aanFeatures.f);
	        anControl.unbind('keyup').bind('keyup', $.debounce( 400, function (e) {
                $.fn.dataTableExt.iApiIndex = i;
                _that.fnFilter(anControl.val());
	        }));
	        return this;
	    });
	    return this;
	};
}

/**
 * Handles querying for pages in a given DataTable object
 * 
 * @param sSource the "sAjaxSource" of the calling table
 * @param aoData the parameters of the DataTable object to send to the server
 * @param fnCallback the function that actually maps the returned page to the DataTable object
 */
function fnPaginationHandler(sSource, aoData, fnCallback) {
	$.post(  
			sSource + jobId + "/comparisons/pagination/"+spaceId+"/"+c1+"/"+c2+"/"+useWallclock,
			aoData,
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