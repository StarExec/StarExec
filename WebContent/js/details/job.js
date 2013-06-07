$(document).ready(function(){
	//$("#solveTblField").hide();
	initUI();
	initDataTables();
	
});


function createDownloadRequest(item,type) {
	createDialog("Processing your download request, please wait. This will take some time for large jobs.");
	token=Math.floor(Math.random()*100000000);
	$(item).attr('href', starexecRoot+"secure/download?token=" +token+ "&type="+ type +"&id="+$("#jobId").attr("value"));
	destroyOnReturn(token);
}

/**
 * Initializes the user-interface
 */
function initUI(){
	$("#jobOutputDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });
	
	$("#jobDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });
	
	$('#jobDownload').unbind("click");
	$('#jobDownload').click(function() {
		createDownloadRequest("#jobDownload","job");		
	});
	
	$('#jobOutputDownload').unbind("click");
	$('#jobOutputDownload').click(function() {
		createDownloadRequest("#jobOutputDownload","j_outputs");
	});
	
	// Job details and job pair tables are open by default
	$('fieldset:first, fieldset:eq(1)').expandable(false);
	$('fieldset:not(:first, :eq(1))').expandable(true);
	
	// Set up row click to send to pair details page
	$("#pairTbl tbody").delegate("tr", "click", function(){
		var pairId = $(this).find('input').val();
		window.location.assign(starexecRoot+"secure/details/pair.jsp?id=" + pairId);
	});
	
}

/**
 * Initializes the DataTable objects
 */
function initDataTables(){
	extendDataTableFunctions();
	
	// Details table
	$('#detailTbl').dataTable( {
		"sDom": 'rt<"bottom"f><"clear">',
		"aaSorting": [],
		"bPaginate": false,        
		"bSort": true        
	});
	
	// Job pairs table
	$('#pairTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/jobs/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
	
	//summary table
	$('#solveTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true
    });
	
	// Change the filter so that it only queries the server when the user stops typing
	$('#pairTbl').dataTable().fnFilterOnDoneTyping();
	
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
	var jobId = getParameterByName('id');
	
	$.post(  
			sSource + jobId + "/pairs/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view job pairs for this job", 5000);
						break;
					default:
						// Replace the current page with the newly received page
						fnCallback(nextDataTablePage);
						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}

function solverPaginationHandler(sSource, aoData, fnCallback) {
	var jobId = getParameterByName('id');
	
	$.post(  
			sSource + jobId + "/solvers/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view job pairs for this job", 5000);
						break;
					default:
						// Replace the current page with the newly received page
						fnCallback(nextDataTablePage);
						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating summary table",5000);
	});
}