var summaryTable;
var pairTable;
var spaceId;
var curSpaceId;
var jobId;
var lastValidSelectOption;
$(document).ready(function(){
	spaceId=$("#spaceId").attr("value");
	jobId=$("#jobId").attr("value");
	initUI();
	initSpaceExplorer();
	initDataTables();
	setInterval(function() {
		pairTable.fnDraw(false);
	},10000);
	reloadTables(spaceId);
});

function createDownloadRequest(item,type,returnIds) {
	createDialog("Processing your download request, please wait. This will take some time for large jobs.");
	token=Math.floor(Math.random()*100000000);
	href = starexecRoot+"secure/download?token=" +token+ "&type="+ type +"&id="+$("#jobId").attr("value");
	if (returnIds!=undefined) {
		href=href+"&returnids="+returnIds;
	}
	$(item).attr('href', href);
	destroyOnReturn(token);
	window.location.href = href;
}


function initSpaceExplorer() {
	// Set the path to the css theme for the jstree plugin
	
	$.jstree._themes = starexecRoot+"css/jstree/";
	var id;
	// Initialize the jstree plugin for the explorer list
	$("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/space/" +jobId+ "/subspaces",	// Where we will be getting json data from 
				"data" : function (n) {
					
					return { id : n.attr ? n.attr("id") : 0}; // What the default space id should be
				} 
			} 
		}, 
		"themes" : { 
			"theme" : "default", 					
			"dots" : true, 
			"icons" : true
		},		
		"types" : {				
			"max_depth" : -2,
			"max_children" : -2,					
			"valid_children" : [ "space" ],
			"types" : {						
				"space" : {
					"valid_children" : [ "space" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/db.png"
					}
				}
			}
		},
		"ui" : {			
			"select_limit" : 1,			
			"selected_parent_close" : "select_parent",			
			"initially_select" : [ "1" ]			
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane
		id = data.rslt.obj.attr("id");
		name = data.rslt.obj.attr("name");
		$("#spaceName").text($('.jstree-clicked').text());
		reloadTables(id);
	}).delegate("a", "click", function (event, data) { event.preventDefault();  });// This just disable's links in the node title	
}

function reloadTables(id) {
	//we  only need to update if we've actually selected a new space
	if (curSpaceId!=id) {
		curSpaceId=id;
		summaryTable.fnClearTable();
		pairTable.fnClearTable();
		$("#solverChoice1").empty();
		$("#solverChoice2").empty();
		$("#spaceOverviewSelections").empty();
		$("#spaceOverview").attr("src",starexecRoot+"/images/emptyGraph.png");
		$("#solverComparison").attr("src",starexecRoot+"/images/emptyGraph.png");
		summaryTable.fnProcessingIndicator(true);
		pairTable.fnProcessingIndicator(true);
		summaryTable.fnReloadAjax(null,null,true,id);
	}

}

function update() {
	
	summaryTable.fnProcessingIndicator(false);
	rows = $("#solveTbl tbody tr");
	rows.each(function() {
	
		$(this).click(function() {
			configId=$(this).find("td:nth-child(2)").children("a:first").attr("id");
			if (typeof(configId!='undefined')) {
				hrefString=starexecRoot + 'secure/details/pairsInSpace.jsp?id=' +jobId+ '&sid='+ curSpaceId+'&configid='+ configId;		
				window.location.href=(hrefString);
			}			
		});
		
		
	});
	
	if (summaryTable.fnSettings().fnRecordsTotal()==0) {
		$("#graphField").hide();
	} else {
		$("#graphField").show();
		$("#spaceOverviewSelections").empty();
		rows.each(function() {
			solverName=$(this).find("a:first").attr("title");
			configName=$(this).find("td:nth-child(2)").children("a:first").attr("title");
			configId=$(this).find("td:nth-child(2)").children("a:first").attr("id");
			htmlString='<option value="' +configId+ '">' +solverName+'/'+configName+ '</ option>';
			$("#spaceOverviewSelections").append(htmlString);
			$("#solverChoice1").append(htmlString);
			$("#solverChoice2").append(htmlString);
		});
		//select first five solver/ configuration pairs
		$("#spaceOverviewSelections").children("option:lt(5)").prop("selected",true);
		lastValidSelectOption = $("#spaceOverviewSelections").val();
		updateSpaceOverview();
		if (summaryTable.fnSettings().fnRecordsTotal()>1) {
			$("#solverComparison").show();
			$("#solverComparisonOptionField").show();
			
			$("#solverChoice1").children("option:first").prop("selected",true);
			$("#solverChoice1").children("option:nth-child(2)").prop("selected",true);
			updateSolverComparison();
		} else {
			$("#solverComparison").hide();
			$("#solverComparisonOptionField").hide();

		}
		
	}
	
	
	
}

/**
 * Initializes the user-interface
 */
function initUI(){
	
	$('#dialog-confirm-delete').hide();
	$('#dialog-confirm-pause').hide();
	$('#dialog-confirm-resume').hide();
	$("#dialog-return-ids").hide();
	$("#dialog-solverComparison").hide();
	$("#dialog-spaceOverview").hide();
	$("#errorField").hide();
	$("#statsErrorField").hide();
	
	//for aesthetics, make the heights of the two option fields identical
	$("#solverComparisonOptionField").height($("#spaceOverviewOptionField").height());
	
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
	
	$('#deleteJob').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	$('#pauseJob').button({
		icons: {
			secondary: "ui-icon-pause"
		}
	});
	
	$('#resumeJob').button({
		icons: {
			secondary: "ui-icon-play"
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
	$('#jobDownload').click(function(e) {
		e.preventDefault();
		$('#dialog-return-ids-txt').text('do you want ids for job pairs, solvers, and benchmarks to be included in the CSV?');
		
		$('#dialog-return-ids').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'yes': function() {
					$('#dialog-return-ids').dialog('close');
					createDownloadRequest("#jobDownload","job",true);		
				},
				"no": function() {
					$('#dialog-return-ids').dialog('close');
					createDownloadRequest("#jobDownload","job",false);		
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});

	});
	
	$('#jobOutputDownload').unbind("click");
	$('#jobOutputDownload').click(function(e) {
		e.preventDefault();
		createDownloadRequest("#jobOutputDownload","j_outputs");
	});
		
	// Set the selected post processor to be the default one
	defaultSolver1 = $('#solverChoice1').attr('default');
	$('#solverChoice1 option[value=' + defaultSolver1 + ']').attr('selected', 'selected');
	
	// Set the selected post processor to be the default one
	defaultSolver2 = $('#solverChoice2').attr('default');
	$('#solverChoice2 option[value=' + defaultSolver2 + ']').attr('selected', 'selected');
	
	//set all fieldsets as expandable
	$('#solverSummaryField').expandable(false);
	$("#pairTblField").expandable(false);
	$("#graphField").expandable(false);
	$("#errorField").expandable(false);
	$("#statsErrorField").expandable(false);
	$("#optionField").expandable(true);
	$("#detailField").expandable(true);
	$("#actionField").expandable(true);
	$("#logScale").change(function() {
		updateSpaceOverview();
	});
	
	lastValidSelectOption = $("#spaceOverviewSelections").val();
	
	$("#spaceOverviewSelections").change(function() {
	        if ($(this).val().length > 5) {
	          showMessage('error',"You may only choose a maximum of 5 solver / configuration pairs to display at one time",5000);
	          $(this).val(lastValidSelectOption);
	        } else {
	        	lastValidSelectOption = $(this).val();
	        	//don't update if nothing is selected, as there would be nothing to display
	  			if ($("#spaceOverviewSelections").children("option:selected").size()>0) {
	  				updateSpaceOverview();
	  			}
	        }
	      
	});
	
	$("#solverChoice1").change(function() {
		updateSolverComparison(false);
	});
	$("#solverChoice2").change(function() {
		updateSolverComparison(false);
	});
	$("#solverComparison").click(function() {
		$('#dialog-solverComparison').dialog({
			modal: true,
			width: 850,
			height: 850
		});
	});
	$("#spaceOverview").click(function() {
		$('#dialog-spaceOverview').dialog({
			modal: true,
			width: 850,
			height: 850
		});
	});
}

function updateSpaceOverview() {
	var configs = new Array();
	$("#spaceOverviewSelections option:selected").each(function() {
		configs.push($(this).attr("value"));
	});
	logY=false;
	if ($("#logScale").prop("checked")) {
		logY=true;
	}
	
	$.post(
			starexecRoot+"services/jobs/" + jobId + "/" + curSpaceId+"/graphs/spaceOverview",
			{logY : logY, selectedIds: configs},
			function(returnCode) {
				
				switch (returnCode) {
				
				case 1:
					showMessage('error',"an internal error occured while processing your request: please try again",5000);
					break;
				case 2:
					showMessage('error',"You do not have sufficient permission to view job pair details for this job in this space",5000);
					break;
				default:
					currentConfigs=new Array();
					$("#spaceOverviewSelections option:selected").each(function() {
						currentConfigs.push($(this).attr("value"));
					});
					//we only want to update the graph if the request we made still matches what the user has put in
					//it is possible the user changed their selections and sent out a new request which has returned already
					//also, equality checking doesn't work on arrays, but less than and greater than do
					if (!(currentConfigs>configs) && !(currentConfigs<configs)) {
						$("#spaceOverview").attr("src",returnCode);
						$("#bigSpaceOverview").attr("src",returnCode+"600");
					}
					
				}
			},
			"text"
	);
}


//big is a boolean that determines whether we should get the big or the small map
function updateSolverComparison(big) {
	config1=$("#solverChoice1 option:selected").attr("value");
	config2=$("#solverChoice2 option:selected").attr("value");
	
	$.post(
			starexecRoot+"services/jobs/" + jobId + "/" + curSpaceId+"/graphs/solverComparison/"+config1+"/"+config2+"/"+big,
			{},
			function(returnCode) {
				
				switch (returnCode) {
				
				case 1:
					showMessage('error',"an internal error occured while processing your request: please try again",5000);
					break;
				case 2:
					showMessage('error',"You do not have sufficient permission to view job pair details for this job",5000);
					break;
				case 12:
					showMessage('error',"you have selected too many solver / configuration pairs",5000);
					break;
				default:
					jsonObject=$.parseJSON(returnCode);
					src=jsonObject.src;
					map=jsonObject.map;
					if (big) {
						$("#bigSolverComparison").attr("src",src);
						$("#bigSolverComparisonMap").remove();
						$("#dialog-solverComparison").append(map);
					} else {
						$("#solverComparison").attr("src",src);
						$("#solverComparisonMap").remove();
						$("#graphField").append(map);
						updateSolverComparison(true);
					}
					
				}
			},
			"text"
	);
}

/**
 * Initializes the DataTable objects
 */
function initDataTables(){
	extendDataTableFunctions();
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
	
	$(".subspaceTable").dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bSort": true,
        "bPaginate": true
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
	
	$('#detailTbl').dataTable( {
		"sDom": 'rt<"bottom"f><"clear">',
		"aaSorting": [],
		"bPaginate": false,        
		"bSort": true        
	});
	
	$('#pairTbl tbody').delegate("a", "click", function(event) {
		event.stopPropogation();
	});
	
	//Set up row click to send to pair details page
	$("#pairTbl tbody").delegate("tr", "click", function(){
		var pairId = $(this).find('input').val();
		window.location.assign(starexecRoot+"secure/details/pair.jsp?id=" + pairId);
	});
	
	// Change the filter so that it only queries the server when the user stops typing
	$('#pairTbl').dataTable().fnFilterOnDoneTyping();
	
}


/**
 * Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
 */
function extendDataTableFunctions(){
	
	// Allows manually turning on and off of the processing indicator (used for jobs table)
	jQuery.fn.dataTableExt.oApi.fnProcessingIndicator = function (oSettings, onoff)	{
		if( typeof(onoff) == 'undefined' ) {
			onoff = true;
		}
		this.oApi._fnProcessingDisplay(oSettings, onoff);
	};
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
	//modified to work for the summary table-- this version of fnReloadAjax should not be used
	//anywhere else
	$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource, fnCallback, bStandingRedraw, id )
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
	    	//if we aren't on the same space anymore, don't update the table with this data.
	    	if (id!=curSpaceId) {
	    		
	    		return;
	    	}
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
	        update();
	    }, oSettings );
	    
	};
}


function fnStatsPaginationHandler(sSource, aoData, fnCallback) {
	var jobId = getParameterByName('id');
	if (curSpaceId==undefined) {
		return;
	}
	outSpaceId=curSpaceId;
	
	$.post(  
			sSource + jobId+"/solvers/pagination/"+outSpaceId,
			aoData,
			function(nextDataTablePage){
				if (outSpaceId!=curSpaceId) {
					return;
				}
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view job pairs for this job", 5000);
						break;
					case 12:
						$("#solverSummaryField").hide();
						$("#graphField").hide();
						$("#statsErrorField").show();
						break;
					default:
						
						$("#solverSummaryField").show();
						$("#graphField").show();
						$("#statsErrorField").hide();
						fnCallback(nextDataTablePage);						
						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
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
	if (curSpaceId==undefined) {
		return;
	}
	outSpaceId=curSpaceId;
	$.post(  
			sSource + jobId + "/pairs/pagination/"+outSpaceId,
			aoData,
			function(nextDataTablePage){
				//do nothing if this is no longer the current request
				if (outSpaceId!=curSpaceId) {
					return;
				}
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view job pairs for this job", 5000);
						break;
					case 12:
						$("#pairTblField").hide();
						$("#errorField").show();
						break;
					default:
						// Replace the current page with the newly received page
						
						pairTable.fnProcessingIndicator(false);
						fnCallback(nextDataTablePage);
						$("#errorField").hide();
						if (pairTable.fnSettings().fnRecordsTotal()==0) {
							$("#pairTblField").hide();
						} else {
							$("#pairTblField").show();
						}
						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}