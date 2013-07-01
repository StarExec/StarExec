var summaryTable;
var pairTable;
$(document).ready(function(){
	initUI();
	initDataTables();
	setInterval(function() {
		pairTable.fnDraw(false);
		summaryTable.fnReloadAjax(null,null,true);
	},10000);
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
	$('#dialog-confirm-delete').hide();
	$('#dialog-confirm-pause').hide();
	$('#dialog-confirm-resume').hide();
	$("#jobOutputDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });
	
	$("#spaceSummary").button({
		icons: {
			primary: "ui-icon-arrowthick-1-e"
		}
	});
	
	$("#jobDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });
	
	$('#deleteJob').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	$('#pauseJob').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	$('#resumeJob').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	$('#dialog-confirm-delete-txt').text('are you sure you want to delete this benchmark?');
	
	$("#deleteJob").click(function(){
		$('#dialog-confirm-delete-txt').text('are you sure you want to delete this job?');
		
		$('#dialog-confirm-delete').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed job deletion.');
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(
							starexecRoot+"services/delete/job/" + getParameterByName("id"),
							function(returnCode) {
								switch (returnCode) {
									case 0:
										window.location = starexecRoot+'secure/explore/spaces.jsp';
										break;
									case 1:
										showMessage('error', "job was not deleted; please try again", 5000);
										break;
									case 2:
										showMessage('error', "only the owner of this job can delete it", 5000);
										break;
									default:
										showMessage('error', "invalid parameters", 5000);
										break;
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled job deletion');
					$(this).dialog("close");
				}
			}
		});
	});
	
	$("#pauseJob").click(function(){
		$('#dialog-confirm-pause-txt').text('are you sure you want to pause this job?');
		
		$('#dialog-confirm-pause').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed job pause.');
					$('#dialog-confirm-pause').dialog('close');
					
					$.post(
							starexecRoot+"services/pause/job/" + getParameterByName("id"),
							function(returnCode) {
								switch (returnCode) {
									case 0:
										//window.location = starexecRoot+'secure/details/job.jsp?id=' +  getParametByName("id");
										document.location.reload(true);
										break;
									case 1:
										showMessage('error', "job was not paused; please try again", 5000);
										break;
									case 2:
										showMessage('error', "only the owner of this job can pause it", 5000);
										break;
									default:
										showMessage('error', "invalid parameters", 5000);
										break;
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled job pause');
					$(this).dialog("close");
				}
			}
		});
	});
	
	$("#resumeJob").click(function(){
		$('#dialog-confirm-resume-txt').text('are you sure you want to resume this job?');
		
		$('#dialog-confirm-resume').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed job resume.');
					$('#dialog-confirm-resume').dialog('close');
					
					$.post(
							starexecRoot+"services/resume/job/" + getParameterByName("id"),
							function(returnCode) {
								switch (returnCode) {
									case 0:
										//window.location = starexecRoot+'secure/details/job.jsp?id=' +  getParametByName("id");
										document.location.reload(true);
										break;
									case 1:
										showMessage('error', "job was not resumed; please try again", 5000);
										break;
									case 2:
										showMessage('error', "only the owner of this job can resume it", 5000);
										break;
									default:
										showMessage('error', "invalid parameters", 5000);
										break;
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled job resume');
					$(this).dialog("close");
				}
			}
		});
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
	
	$('#pairTbl tbody').delegate("a", "click", function(event) {
		event.stopPropogation();
	});

	
	//Set up row click to send to pair details page
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
	pairTable=$('#pairTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "sAjaxSource"	: starexecRoot+"services/jobs/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
	
	//summary table
	summaryTable=$('#solveTbl').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true,
        "sAjaxSource"	: starexecRoot+"services/jobs/",
        "sServerMethod" : "POST",
        "fnServerData" : fnStatsPaginationHandler
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
	
	//allows refreshing a table that is using client-side processing (for the summary table)
	$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource, fnCallback, bStandingRedraw )
	{
	    if ( sNewSource !== undefined && sNewSource !== null ) {
	        oSettings.sAjaxSource = sNewSource;
	    }
	 
	    // Server-side processing should just call fnDraw
	    if ( oSettings.oFeatures.bServerSide ) {
	        this.fnDraw();
	        return;
	    }
	 
	    this.oApi._fnProcessingDisplay( oSettings, true );
	    var that = this;
	    var iStart = oSettings._iDisplayStart;
	    var aData = [];
	 
	    this.oApi._fnServerParams( oSettings, aData );
	 
	    oSettings.fnServerData.call( oSettings.oInstance, oSettings.sAjaxSource, aData, function(json) {
	        /* Clear the old information from the table */
	        that.oApi._fnClearTable( oSettings );
	 
	        /* Got the data - add it to the table */
	        var aData =  (oSettings.sAjaxDataProp !== "") ?
	            that.oApi._fnGetObjectDataFn( oSettings.sAjaxDataProp )( json ) : json;
	 
	        for ( var i=0 ; i<aData.length ; i++ )
	        {
	            that.oApi._fnAddData( oSettings, aData[i] );
	        }
	         
	        oSettings.aiDisplay = oSettings.aiDisplayMaster.slice();
	 
	        that.fnDraw();
	 
	        if ( bStandingRedraw === true )
	        {
	            oSettings._iDisplayStart = iStart;
	            that.oApi._fnCalculateEnd( oSettings );
	            that.fnDraw( false );
	        }
	 
	        that.oApi._fnProcessingDisplay( oSettings, false );
	 
	        /* Callback user function - for event handlers etc */
	        if ( typeof fnCallback == 'function' && fnCallback !== null )
	        {
	            fnCallback( oSettings );
	        }
	    }, oSettings );
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

function fnStatsPaginationHandler(sSource, aoData, fnCallback) {
	var jobId = getParameterByName('id');
	
	$.post(  
			sSource + jobId+"/solvers/pagination",
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