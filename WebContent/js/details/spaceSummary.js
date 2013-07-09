var summaryTable;
var pairTable;
var spaceId;
var curSpaceId;
var jobId;
$(document).ready(function(){
	spaceId=$("#spaceId").attr("value");
	curSpaceId=spaceId;
	jobId=$("#jobId").attr("value");
	initUI();
	initDataTables();
	initSpaceExplorer();
});


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
		reloadSummaryTable(id);
	}).delegate("a", "click", function (event, data) { event.preventDefault();  });// This just disable's links in the node title	
}

function reloadSummaryTable(id) {
	curSpaceId=id;
	summaryTable.fnReloadAjax(null,null,null,true,id);
}

function update(id) {
	$("#solverChoice1").empty();
	$("#solverChoice2").empty();
	rows = $("#solveTbl tbody tr");
	rows.each(function() {
		configId=$(this).find("td:nth-child(2)").children("a:first").attr("id");
		if (typeof(configId)=='undefined') {
			
		} else {
			$(this).click(function() {
				window.location.href=(starexecRoot + 'secure/details/pairsInSpace.jsp?id=' +jobId+ '&sid='+ curSpaceId+'&configid='+ configId);
			});
		}
		
	});
	if (summaryTable.fnSettings().fnRecordsTotal()==0) {
		$("#graphField").hide();
	} else {
		$("#graphField").show();
		updateSpaceOverview();
	}
	if (rows.length>1) {
		$("#solverComparison").show();
		$("#solverChoice1").show();
		$("#solverChoice2").show();
		rows.each(function() {
			solverName=$(this).find("a:first").attr("title");
			configName=$(this).find("td:nth-child(2)").children("a:first").attr("title");
			configId=$(this).find("td:nth-child(2)").children("a:first").attr("id");
			$("#solverChoice1").append('<option value="' +configId+ '">' +solverName+'/'+configName+ '</ option>');
			$("#solverChoice2").append('<option value="' +configId+ '">' +solverName+'/'+configName+ '</ option>');
		});
		$("#solverChoice1").children("option:first").prop("selected",true);
		$("#solverChoice1").children("option:nth-child(2)").prop("selected",true);
		updateSolverComparison();
	} else {
		$("#solverComparison").hide();
		$("#solverChoice1").hide();
		$("#solverChoice2").hide();
	}
}

/**
 * Initializes the user-interface
 */
function initUI(){
		
	// Set the selected post processor to be the default one
	defaultSolver1 = $('#solverChoice1').attr('default');
	$('#solverChoice1 option[value=' + defaultSolver1 + ']').attr('selected', 'selected');
	
	// Set the selected post processor to be the default one
	defaultSolver2 = $('#solverChoice2').attr('default');
	$('#solverChoice2 option[value=' + defaultSolver2 + ']').attr('selected', 'selected');
	
	//set all fieldsets as expandable
	$('fieldset').expandable(true);
	
	$("#logScale").change(function() {
		updateSpaceOverview(logY);
	});
	
	$("#solverChoice1").change(function() {
		updateSolverComparison();
	});
	$("#solverChoice2").change(function() {
		updateSolverComparison();
	});
}

function updateSpaceOverview() {
	logY=false;
	if ($("#logScale").prop("checked")) {
		logY=true;
	}
	$.post(
			starexecRoot+"services/jobs/" + jobId + "/" + curSpaceId+"/graphs/spaceOverview",
			{logY : logY},
			function(returnCode) {
				
				switch (returnCode) {
				
				case 1:
					showMessage('error',"an internal error occured while processing your request: please try again",5000);
					break;
				case 2:
					showMessage('error',"You do not have sufficient permission to view job pair details for this job in this space",5000);
					break;
				default:
					$("#spaceOverview").attr("src",returnCode);
					$("#spaceOverviewLink").attr("src",returnCode+"600");
				}
			},
			"text"
	);
}

function updateSolverComparison() {
	config1=$("#solverChoice1 option:selected").attr("value");
	
	config2=$("#solverChoice2 option:selected").attr("value");
	$.post(
			starexecRoot+"services/jobs/" + jobId + "/" + curSpaceId+"/graphs/solverComparison/"+config1+"/"+config2,
			{},
			function(returnCode) {
				
				switch (returnCode) {
				
				case 1:
					showMessage('error',"an internal error occured while processing your request: please try again",5000);
					break;
				case 2:
					showMessage('error',"You do not have sufficient permission to view job pair details for this job in this space",5000);
					break;
				default:
					jsonObject=$.parseJSON(returnCode);
					src=jsonObject.src;
					map=jsonObject.map;
					
					$("#solverComparison").attr("src",src);
					$("#solverComparisonLink").attr("href",src+"600");
					$("#solverComparisonMap").remove();
					$("#graphField").append(map);
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
	        update(id);
	    }, oSettings );
	    
	};
}


function fnStatsPaginationHandler(sSource, aoData, fnCallback) {
	var jobId = getParameterByName('id');
	if (curSpaceId==undefined) {
		curSpaceId=spaceId;
	}
	$.post(  
			sSource + jobId+"/solvers/pagination/"+curSpaceId,
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
						update(curSpaceId);
						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}