var summaryTable; 
var pairTable;
var curSpaceId; //stores the ID of the job space that is currently selected from the space viewer
var jobId; //the ID of the job being viewed
var lastValidSelectOption;
var panelArray=null;
var useWallclock=true;
var syncResults=false;
var DETAILS_JOB = {}; 

$(document).ready(function(){
	initializeGlobalPageVariables();
	//sets up buttons and so on
	initUI();
	

	initSpaceExplorer();

	initDataTables();

	if (!isLocalJobPage) {
		//update the tables every 30 seconds
		setInterval(function() {
			pairTable.fnDraw(false);
			refreshPanels();
		},30000);

		//puts data into the data tables
		reloadTables($("#spaceId").attr("value"));
	}
});

// Initializes the fields of the global DETAILS_JOB object.
function initializeGlobalPageVariables() {
	'use strict';

	// Set these first since get functions may depend no them
	DETAILS_JOB.isAnonymousPage = $('#isAnonymousPage').attr('value') === 'true';
	jobId=$('#jobId').attr('value');
	DETAILS_JOB.starexecUrl = $('#starexecUrl').attr('value');
	DETAILS_JOB.rootJobSpaceId = $('#spaceId').attr('value');
	DETAILS_JOB.anonymizeNames = $('#anonymizeNames').attr('value') === 'true';
	DETAILS_JOB.anonymousLinkUuid = getParameterByName('anonId');

	DETAILS_JOB.solverTableInitializer = getSolverTableInitializer();
	DETAILS_JOB.pairTableInitializer = getPairTableInitializer();
	DETAILS_JOB.spaceExplorerJsonData = getSpaceExplorerJsonData();

	log("starexecUrl: " + DETAILS_JOB.starexecUrl);
	log("isLocalJobPage: " + isLocalJobPage);
	log("starexecRoot: " + starexecRoot);
	log( 'isAnonymousPage: ' + DETAILS_JOB.isAnonymousPage );
	log( 'anonymizeNames: ' + DETAILS_JOB.anonymizeNames );
}

function getPanelTableInitializer(jobId, spaceId) {
	'use strict';
	var panelTableInitializer = null;

	var paginationUrl = '';
	if ( DETAILS_JOB.isAnonymousPage ) {
		paginationUrl = starexecRoot+'services/jobs/solvers/anonymousLink/pagination/'+spaceId+'/'+DETAILS_JOB.anonymousLinkUuid+'/'+
				DETAILS_JOB.anonymizeNames+'/true/';
	} else {
		paginationUrl = starexecRoot+'services/jobs/solvers/pagination/'+spaceId+'/true/';
	}

	if (isLocalJobPage && useWallclock) {
		// get the JSON directly from the page if this is a local page.
		panelTableInitializer = $.parseJSON($('#jobSpaceWallclockTimeSolverStats'+spaceId).attr('value'));
	} else if (isLocalJobPage && !useWallclock) {
		// get the JSON directly from the page if this is a local page.
		panelTableInitializer = $.parseJSON($('#jobSpaceCpuTimeSolverStats'+spaceId).attr('value'));
	} else {
		// otherwise get it from the server
		panelTableInitializer = {
			'sDom'			: 'rt<"clear">',
			'iDisplayStart'	: 0,
			'iDisplayLength': 1000, // make sure we show every entry
			'sAjaxSource'	: paginationUrl,
			'sServerMethod' : 'POST',
			'fnServerData'  : fnShortStatsPaginationHandler
		};
	}

	return panelTableInitializer;
}


// Gets the JSON representation of the space explorer from the hidden <span> containing it.
function getSpaceExplorerJsonData() {
	'use strict';
	var spaceExplorerJsonData = {};
	if (isLocalJobPage) {
		var jobSpaceTreeJsonText = $('#jobSpaceTreeJson').attr('value');
		try {
			spaceExplorerJsonData = {
				"data": JSON.parse(jobSpaceTreeJsonText)
			};
		} catch (syntaxError) {
			log("Caught syntax error when trying to call JSON.parse on: "+jobSpaceTreeJsonText);
			return {};
		}
	} else {
		var url = '';
		if ( DETAILS_JOB.isAnonymousPage ) {
			url = starexecRoot+"services/space/anonymousLink/" +DETAILS_JOB.anonymousLinkUuid+ "/jobspaces/true/"+DETAILS_JOB.anonymizeNames;
		} else {
			url = starexecRoot+"services/space/" +jobId+ "/jobspaces/true";
		}
		spaceExplorerJsonData = {
			"ajax" : { 
				"url" : url, // Where we will be getting json data from 
				"data" : function (n) {
					return {
						id : (n.attr ? n.attr("id") : 0)
					}; // What the default space id should be
				} 
			} 
		};
	}
	return spaceExplorerJsonData;
}

function setSyncResultsText() {
	if (syncResults) {
		$("#syncResults .ui-button-text").html("un-synchronize results");
	} else {
		$("#syncResults .ui-button-text").html("synchronize results");

	}
}

function refreshPanels(){
	for (i=0;i<panelArray.length;i++) {
		panelArray[i].api().ajax.reload(null,true);
	}
}

function refreshStats(id){
	//summaryTable.fnProcessingIndicator(true);
	summaryTable.api().ajax.reload(function() {
		updateGraphs();
	},true);
}

function createDownloadRequest(item,type,returnIds,getCompleted) {
	createDialog("Processing your download request, please wait. This will take some time for large jobs.");
	var token=Math.floor(Math.random()*100000000);
	var href = DETAILS_JOB.starexecUrl+"secure/download?token=" +token+ "&type="+ type +"&id="+$("#jobId").attr("value");
	if (typeof returnIds!= 'undefined' ) {
		href=href+"&returnids="+returnIds;
	}
	if (typeof getCompleted!='undefined') {
		href=href+"&getcompleted="+getCompleted;
	}
	$(item).attr('href', href);
	destroyOnReturn(token);		//when we see the download token as a cookie, destroy the dialog box
	window.location.href = href;
}


function initSpaceExplorer() {
	// Set the path to the css theme for the jstree plugin
	
	$.jstree._themes = starexecRoot+"css/jstree/";
	var id;

	// Initialize the jstree plugin for the explorer list
	/*$("#exploreList").bind("loaded.jstree", function() {
		log("exploreList tree has finished loading.");
		$("#exploreList").jstree("select_node", ".rootNode");
	})*/
	$("#exploreList").jstree({  
		"json_data" : DETAILS_JOB.spaceExplorerJsonData, 
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
			"initially_select" : [ "#"+DETAILS_JOB.rootJobSpaceId ]			
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
			// When a node is clicked, get its ID and display the info in the details pane
			id = data.rslt.obj.attr("id");
			name = data.rslt.obj.attr("name");
			maxStages = data.rslt.obj.attr("maxStages");
			setMaxStagesDropdown(parseInt(maxStages));
			$("#spaceName").text($('.jstree-clicked').text());
			$("#displayJobSpaceID").text("job space id  = "+id);
			//no solvers will be selected when a space changes, so hide this button
			$("#compareSolvers").hide();
			reloadTables(id);
	}).on( "click", "a", function (event, data) { 
		event.preventDefault();  // This just disable's links in the node title	
	});
	log("Initialized exploreList tree.");

}

function clearPanels() {
	if (panelArray==null)  {
		return;
	}
	for (i=0;i<panelArray.length;i++) {
		panelArray[i].fnDestroy();
		$(panelArray[i]).remove();
	}
	$(".panelField").remove();
	panelArray=null;
}

function reloadTables(id) {
	//we only need to update if we've actually selected a new space 
	if (curSpaceId!=id) {
		curSpaceId=id;
		clearPanels();
		if (!isLocalJobPage) {
			summaryTable.fnClearTable();	//immediately get rid of the current data, which makes it look more responsive
			pairTable.fnClearTable();
		
			//clear out the graphs
			$("#solverChoice1").empty();
			$("#solverChoice2").empty();
			$("#spaceOverviewSelections").empty();
			$("#spaceOverview").attr("src",starexecRoot+"/images/loadingGraph.png");
			$("#solverComparison").attr("src",starexecRoot+"/images/loadingGraph.png");
			
			//tell the tables to display a "loading" indicator
			summaryTable.fnProcessingIndicator(true);
			
			pairTable.fnProcessingIndicator(true);
			refreshStats(id);
			
		} else {
			$('[id$=pairTbl_wrapper]').hide();
			$('#pairTblField').show();
			$('#'+id+'pairTbl_wrapper').show();
			$('[id$=solveTbl_wrapper]').hide();
			$('#'+id+'solveTbl_wrapper').show();
			log('showing id: '+id);
		}
		initializePanels();
	}
}

function updateGraphs() {
	
	summaryTable.fnProcessingIndicator(false);

	rows = summaryTable.fnGetNodes();
	if (summaryTable.fnSettings().fnRecordsTotal()==0) {
		$("#graphField").hide();
	} else {
		$("#graphField").show();
		$("#spaceOverviewSelections").empty();
		$(rows).each(function() {
			//alert(this.html());
			var solverName=$(this).find("a:first").attr("title");
			var configName=$(this).find("td:nth-child(2)").children("a:first").attr("title");
			var configId=$(this).find("td:nth-child(2)").children("a:first").attr("id");
			var htmlString='<option value="' +configId+ '">' +solverName+'/'+configName+ '</ option>';
			$("#spaceOverviewSelections").append(htmlString);
			$("#solverChoice1").append(htmlString);
			$("#solverChoice2").append(htmlString);
		});
		//select first five solver/ configuration pairs
		$("#spaceOverviewSelections").children("option:lt(5)").prop("selected",true);
		lastValidSelectOption = $("#spaceOverviewSelections").val();
		updateSpaceOverviewGraph();
		if (summaryTable.fnSettings().fnRecordsTotal()>1) {
			$("#solverComparison").show();
			$("#solverComparisonOptionField").show();
			
			$("#solverChoice1").children("option:first").prop("selected",true);
			$("#solverChoice1").children("option:nth-child(2)").prop("selected",true);
			updateSolverComparison(300, "white");
		} else {
			$("#solverComparison").hide();
			$("#solverComparisonOptionField").hide();

		}
		
	}	
}

function makeJobNameUneditable() {
	// Gets rid of the '(click to edit)' part of the text.
	$('#jobNameTitle').text('name');
	$('#editJobNameWrapper').hide();
}

function makeJobDescriptionUneditable() {
	$('#jobDescriptionTitle').text('description');
	$('#editJobDescriptionWrapper').hide();
}

//
//  Initializes the user-interface
// 
function initUI(){
	$('#dialog-confirm-delete').hide();
	$('#dialog-confirm-pause').hide();
	$('#dialog-confirm-resume').hide();
	$( "#dialog-warning").hide();
	$("#dialog-return-ids").hide();
	$("#dialog-solverComparison").hide();
	$("#dialog-spaceOverview").hide();
	$("#dialog-postProcess").hide();
	$("#dialog-changeQueue").hide();
	$("#errorField").hide();
	$("#statsErrorField").hide();
	$(".cpuTime").hide();

	if (!isLocalJobPage) {
		setupJobNameAndDescriptionEditing('#jobNameText', '#editJobName', '#editJobNameButton', '#editJobNameWrapper', 'name');
		setupJobNameAndDescriptionEditing('#jobDescriptionText', '#editJobDescription', '#editJobDescriptionButton', '#editJobDescriptionWrapper', 
				'description');
		registerAnonymousLinkButtonEventHandler();
	} else {
		makeJobNameUneditable();
		makeJobDescriptionUneditable();
	}

	if (isLocalJobPage) {
		$('#actionField').hide();
		$('#matrixViewButton').hide();
		$('#downloadJobPageButton').hide();
		$('#anonymousLink').hide();
	}

	//for aesthetics, make the heights of the two option fields identical
	$("#solverComparisonOptionField").height($("#spaceOverviewOptionField").height());
	
	$("#jobOutputDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });


	$("#compareSolvers").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });
	$("#compareSolvers").hide();
	
	$("#compareSolvers").click(function(){
		'use strict';
		var c1=$(".first_selected").find(".configLink").attr("id");
		var c2=$(".second_selected").find(".configLink").attr("id");
		window.open(DETAILS_JOB.starexecUrl+"secure/details/solverComparison.jsp?id="+jobId+"&sid="+curSpaceId+"&c1="+c1+"&c2="+c2);
	});

	
	
	attachSortButtonFunctions();
	
	$("#rerunPairs").button({
		icons: {
			primary: "ui-icon-arrowreturnthick-1-e"
		}
	});
		
	$("#jobXMLDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
	});
	
	$('#clearCache').button( {
		icons: {
			secondary: "ui-icon-arrowrefresh-1-e"
		}
	});
	$("#clearCache").click(function(){
			
			$("#dialog-warning-txt").text('Are you sure you want to clear the cache for this primitive?');		
			$("#dialog-warning").dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'clear cache': function() {
						$(this).dialog("close");
							$.post(
									starexecRoot+"services/cache/clear/stats/"+jobId+"/",
									function(returnCode) {
										s=parseReturnCode(returnCode);	
							});
															
					},
					"cancel": function() {
						$(this).dialog("close");
					}
				}
			});
	});
	
	$('#recompileSpaces').button( {
		icons: {
			secondary: "ui-icon-arrowrefresh-1-e"
		}
	});
	$("#recompileSpaces").click(function() {
		$.get(
				starexecRoot+"services/recompile/"+jobId,
				function(returnCode) {
					s=parseReturnCode(returnCode);	
		});
	});
	
	
	$("#popoutPanels").button({
		icons: {
			primary: "ui-icon-extlink"
		}
	});
	$("#collapsePanels").button( {
		icons: {
			primary: "ui-icon-folder-collapsed"
		}
	}) ;
	$("#openPanels").button( {
		icons: {
			primary: "ui-icon-folder-open"
		}
	}) ;
	$(".changeTime").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	
	});

	$("#matrixViewButton").button({
		icons: {
			primary: "ui-icon-newwin"
		}
	});

	$("#downloadJobPageButton").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
	});

	$("#downloadJobPageButton").click(function() {
		createDownloadRequest("#downloadJobPageButton", "job_page");
	});

	
	$("#syncResults").button({
		icons: {
			primary: "ui-icon-gear"
		}
	});

	$("#matrixViewButton").click(function() {
		var url = DETAILS_JOB.starexecUrl+'secure/details/jobMatrixView.jsp?id='+jobId+'&stage=1&jobSpaceId='+curSpaceId;
		if (isLocalJobPage) {
			window.location.href = url;
		} else {
			popup(url);
		}
	});


	$("#syncResults").click(function() {
		//just change the sync results boolean and update the button text.
		syncResults=!syncResults;
		setSyncResultsText();
		pairTable.fnDraw(false);
	});
	
	$("#spaceOverviewUpdate").button({
		icons: {
			primary: "ui-icon-arrowrefresh-1-e"
		}
	});
	$("#solverComparisonUpdate").button({
		icons: {
			primary: "ui-icon-arrowrefresh-1-e"
		}
	});
	$("#jobDownload").button({
		icons: {
			primary: "ui-icon-arrowthick-1-s"
		}
    });

	
	$("#popoutPanels").click(function() {
		// default to primary stage
		window.open(DETAILS_JOB.starexecUrl+"secure/details/jobPanelView.jsp?jobid="+jobId+"&spaceid="+curSpaceId+"&stage=1");
	});

	$("#collapsePanels").click(function() {
		$(".panelField").each(function() {
			legend = $(this).children('legend:first');
			isOpen = $(legend).data('open');
			if (isOpen) {
				$(legend).trigger("click");
			}
		});
	});



	$("#openPanels").click(function() {
		$(".panelField").each(function() {
			legend = $(this).children('legend:first');
			isOpen = $(legend).data('open');
			
			if (!isOpen) {
				$(legend).trigger("click");
			}
		});
	});

		$(".changeTime").click(function() {
			useWallclock=!useWallclock;
			if (useWallclock) {
				$('.cpuTime').hide();
				$('.wallclockTime').show();
			} else {
				$('.cpuTime').show();
				$('.wallclockTime').hide();
			}
			setTimeButtonText();
			if (!isLocalJobPage) {
				refreshPanels();
				refreshStats(curSpaceId);
				pairTable.fnDraw(false);
			}
		});

	if (!isLocalJobPage) {
		$(".stageSelector").change(function() {
			//set the value of all .stageSelectors to this one to sync them.
			//this does not trigger the change event, which is good because it would loop forever
			$(".stageSelector").val($(this).val());
			pairTable.fnDraw(false);
			refreshPanels();
			refreshStats(curSpaceId);
		});
	}

	setupDeleteJobButton();
	setupPauseJobButton();
	setupResumeJobButton();
	setupChangeQueueButton();
	setupPostProcessButton();
	
	$('#jobDownload').unbind("click");
	$('#jobDownload').click(function(e) {
		e.preventDefault();
		$('#dialog-return-ids-txt').text('do you want ids for job pairs, solvers, and benchmarks to be included in the CSV?');
		
		$('#dialog-return-ids').dialog({
			modal: true,
			width: 380,
			height: 200,
			buttons: {
				'download': function() {
					$('#dialog-return-ids').dialog('close');
					createDownloadRequest("#jobDownload","job",$("#includeids").prop("checked"),$("#getcompleted").prop("checked"));		
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});

	});

	$('#jobXMLDownload').unbind("click");
	$('#jobXMLDownload').click(function(e) {
		e.preventDefault();
		createDownloadRequest("#jobXMLDownload","jobXML");
	});
	
	$('#jobOutputDownload').unbind("click");
	$('#jobOutputDownload').click(function(e) {
		e.preventDefault();
		createDownloadRequest("#jobOutputDownload","j_outputs");
	});
		
	//set teh two default solvers to compare
	var defaultSolver1 = $('#solverChoice1').attr('default');
	$('#solverChoice1 option[value=' + defaultSolver1 + ']').prop('selected', true);
	
	var defaultSolver2 = $('#solverChoice2').attr('default');
	$('#solverChoice2 option[value=' + defaultSolver2 + ']').prop('selected', true);
	
	//set all fieldsets as expandable
	$('#solverSummaryField').expandable(false);
	$("#pairTblField").expandable(false);
	$("#graphField").expandable(false);
	$("#errorField").expandable(false);
	$("#statsErrorField").expandable(false);
	$("#optionField").expandable(true);
	$("#detailField").expandable(true);
	$("#actionField").expandable(true);
	
	
	$("#subspaceSummaryField").expandable(false);
	
	lastValidSelectOption = $("#spaceOverviewSelections").val();

	if (!isLocalJobPage) {
		$("#spaceOverviewUpdate").click(function() {
			updateSpaceOverviewGraph();
		});
		$("#solverComparisonUpdate").click(function() {
			updateSolverComparison(300, "white");
		});
	}

	$("#spaceOverviewSelections").change(function() {
	        if ($(this).val().length > 5) {
	          showMessage('error',"You may only choose a maximum of 5 solver / configuration pairs to display at one time",5000);
	          $(this).val(lastValidSelectOption);
	        } else {
	        	lastValidSelectOption = $(this).val();
	        	
	        }
	      
	});
	$("#solverComparison300").click(function() {
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

	setTimeButtonText();
}

function setupDeleteJobButton() {
	'use strict';
	$('#deleteJob').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
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
							starexecRoot+"services/delete/job",
							{selectedIds: [getParameterByName("id")]},
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									window.location = starexecRoot+'secure/explore/spaces.jsp';

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
}

function setupPauseJobButton() {
	'use strict';
	$('#pauseJob').button({
		icons: {
			secondary: "ui-icon-pause"
		}
	});
	$("#pauseJob").click(function(){
		
		$.post(
				starexecRoot+"services/pause/job/" + getParameterByName("id"),
				function(returnCode) {
					s=parseReturnCode(returnCode);
					if (s) {
						document.location.reload(true);

					}
					
				},
				"json"
		);
	});
}

function setupResumeJobButton() {
	'use strict';
	$('#resumeJob').button({
		icons: {
			secondary: "ui-icon-play"
		}
	});
	$("#resumeJob").click(function(){
		$.post(
				starexecRoot+"services/resume/job/" + getParameterByName("id"),
				function(returnCode) {
					s=parseReturnCode(returnCode);
					if (s) {
						document.location.reload(true);

					}
					
				},
				"json"
		);
	});
}

function setupChangeQueueButton() {
	'use strict';
	$('#changeQueue').button({
		icons: {
			secondary: "ui-icon-transferthick-e-w"
		}
	});
	$("#changeQueue").click(function(){
		$('#dialog-changeQueue-txt').text('Please select a new queue to use for this job.');
		
		$('#dialog-changeQueue').dialog({
			modal: true,
			width: 380,
			height: 200,
			buttons: {
				'OK': function() {
					$('#dialog-changeQueue').dialog('close');
					$.post(
							starexecRoot+"services/changeQueue/job/" + getParameterByName("id")+"/"+$("#changeQueueSelection").val(),
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									setTimeout(function(){document.location.reload(true);}, 1000);

								}
							},
							"json"
					);
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});
	});
}

function setupPostProcessButton() {
	'use strict';
	$("#postProcess").button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
	});
	$("#postProcess").click(function(){
		$('#dialog-postProcess-txt').text('Please select a post-processor to use for this job.');
		
		$('#dialog-postProcess').dialog({
			modal: true,
			width: 380,
			height: 200,
			buttons: {
				'OK': function() {
					$('#dialog-postProcess').dialog('close');
					showMessage("info","Beginning job pair processing. ",3000);
					$.post(
							starexecRoot+"services/postprocess/job/" + getParameterByName("id")+"/"+$("#postProcessorSelection").val()+"/"+getSelectedStage(),
							function(returnCode) {
								parseReturnCode(returnCode);
								
							},
							"json"
					);
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});
	});
}


function updateSpaceOverviewGraph() {
	var configs = new Array();
	$("#spaceOverviewSelections option:selected").each(function() {
		configs.push($(this).attr("value"));
	});
	logY=false;
	if ($("#logScale").prop("checked")) {
		logY=true;
	}

	var postUrl = null;
	if ( DETAILS_JOB.isAnonymousPage ) {
		postUrl = starexecRoot+'services/jobs/anonymousLink/'+ DETAILS_JOB.anonymousLinkUuid +'/' + curSpaceId + 
				'/graphs/spaceOverview/'+getSelectedStage()+'/'+DETAILS_JOB.anonymizeNames;
	} else {
		postUrl = starexecRoot+'services/jobs/' + curSpaceId+'/graphs/spaceOverview/'+getSelectedStage();
	}
	log('updateSpaceOverviewGraph postUrl: ' + postUrl);
	
	$.post(
			postUrl,
			{logY : logY, selectedIds: configs},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
					currentConfigs=new Array();
					$("#spaceOverviewSelections option:selected").each(function() {
						currentConfigs.push($(this).attr("value"));
					});
					//we only want to update the graph if the request we made still matches what the user has put in
					//it is possible the user changed their selections and sent out a new request which has returned already
					//also, equality checking doesn't work on arrays, but less than and greater than do
					if (!(currentConfigs>configs) && !(currentConfigs<configs)) {
						$("#spaceOverview").attr("src",returnCode);
						$("#bigSpaceOverview").attr("src",returnCode+"800");
					}
				} else {
					$("#spaceOverview").attr("src",starexecRoot+"/images/noDisplayGraph.png");

				}
				
					
					
					
				
			},
			"text"
	);
}

//size is in pixels and color is a string
function updateSolverComparison(size, color) {
	var config1=$("#solverChoice1 option:selected").attr("value");
	var config2=$("#solverChoice2 option:selected").attr("value");

	var postUrl = '';
	if ( DETAILS_JOB.isAnonymousPage ) {
		postUrl = starexecRoot+"services/jobs/anonymousLink/"+DETAILS_JOB.anonymousLinkUuid+"/"+curSpaceId+"/graphs/solverComparison/"+config1+
			"/"+config2+"/"+size+"/"+color+"/"+getSelectedStage()+"/"+DETAILS_JOB.anonymizeNames;
	} else {
		postUrl = starexecRoot+"services/jobs/"+curSpaceId+"/graphs/solverComparison/"+config1+"/"+config2+"/"+size+"/"+color+"/"+getSelectedStage();
	}
	
	$.post(
			postUrl,
			{},
			function(returnCode) {
				s=parseReturnCode(returnCode);
				if (s) {
					var jsonObject=$.parseJSON(returnCode);
					var src=jsonObject.src;
					var map=jsonObject.map;
					$("#solverComparison"+size).attr("src",src);
					$("#solverComparisonMap"+size).remove();
					if (size==800) {
						$("#dialog-solverComparison").append(map);
					} else {
						$("#graphField").append(map);
						updateSolverComparison(800, "black");
					}
					
				} else {
					$("#solverComparison300").attr("src",starexecRoot+"/images/noDisplayGraph.png");

				}
				
			},
			"text"
	);
}

function setupJobNameAndDescriptionEditing(textSelector, inputSelector, buttonSelector, wrapperSelector, nameOrDescription) {
	// Hide the wrapper when the page loads.
	$(wrapperSelector).hide();
	// Setup the button when the page loads.
	$(buttonSelector).button();

	$(textSelector).click(function() {
		$(textSelector).hide();
		$(wrapperSelector).show();
		$(inputSelector).select();
	});

	// Had to use mousedown here so that it would precede $('editJobName').blur()
	$(buttonSelector).mousedown(function() {
		log('Attempting to change name...');
		var name = $(inputSelector).val();
		// Make sure the name is a valid primitive name.
		var primRegex = null;
		if (nameOrDescription === 'name') {
			primRegex =new RegExp(getPrimNameRegex());
		} else {
			primRegex = new RegExp(getPrimDescRegex());
		}
		if (!primRegex.test(name)) {
			showMessage("error", "The given "+nameOrDescription+" contains illegal characters.", 5000);
			return;
		}
		$.post(
			starexecRoot+'services/job/edit/'+nameOrDescription+'/'+jobId+'/'+name,
			{},
			function(returnCode) {
				success = parseReturnCode(returnCode);
				if (success) {
					$(textSelector).text(name);
					$(inputSelector).val(name);
					if (nameOrDescription === 'name') {
						// Change the title of the page to the new name.
						$('#mainTemplateHeader').text(name);
					}
				}
			},
			'json'
		);	
		$(wrapperSelector).hide();
		$(textSelector).show();		
	});	

	$(inputSelector).blur(function() {
		$(wrapperSelector).hide();
		$(textSelector).show();
		$(inputSelector).val($(textSelector).text());
	});
}

function openSpace(childId) {
	$("#exploreList").jstree("open_node", "#" + curSpaceId, function() {
		$.jstree._focused().select_node("#" + childId, true);	
	});	
}


function getPanelTable(space) {
	spaceName=space.attr("name");
	spaceId=parseInt(space.attr("id"));
	
	table="<fieldset class=\"panelField\">" +
			"<legend class=\"panelHeader\">"+spaceName+"</legend>" +
			"<table id=panel"+spaceId+" spaceId=\""+spaceId+"\" class=\"panel\"><thead>" +
					"<tr class=\"viewSubspace\"><th colspan=\"4\" >Go To Subspace</th></tr>" +
			"<tr><th class=\"solverHead\">solver</th><th class=\"configHead\">config</th> " +
			"<th class=\"solvedHead\" title=\"Number of job pairs for which the result matched the expected result, or those attributes are undefined, over the number of job pairs that completed without any system errors\">solved</th> " +
			"<th class=\"timeHead\" title=\"total wallclock or cpu time for all job pairs run that were solved correctly\">time</th> </tr>" +
			"</thead>" +
			"<tbody></tbody> </table></fieldset>";
	return table;
	
}

function initializePanels() {
	'use strict';
	DETAILS_JOB.sentSpaceId=curSpaceId;
	if (isLocalJobPage) {
		var panelJson = $.parseJSON($("#subspacePanelJson"+DETAILS_JOB.sentSpaceId).attr("value"));
		handleSpacesData(panelJson);
	} else if ( DETAILS_JOB.isAnonymousPage ) {
		//TODO SPAGETT
		$.getJSON(starexecRoot+"services/space/anonymousLink/"+DETAILS_JOB.anonymousLinkUuid + "/jobspaces/false/"+DETAILS_JOB.anonymizeNames+"?id="+DETAILS_JOB.sentSpaceId, handleSpacesData);
	} else {
		$.getJSON(starexecRoot+"services/space/" +jobId+ "/jobspaces/false?id="+DETAILS_JOB.sentSpaceId, handleSpacesData);
	}
}

function handleSpacesData(spaces) {
	log( "SPACES JSON: " + spaces );
	panelArray=new Array();
	var open=true;
	if (spaces.length==0) {
		$("#subspaceSummaryField").hide();
	}else {
		$("#subspaceSummaryField").show();
		
	}
	
	for (i=0;i<spaces.length;i++) {
		
		space=$(spaces[i]);
		spaceName=space.attr("name");
		spaceId=parseInt(space.attr("id"));
		
		child=getPanelTable(space);
		//if the user has changed spaces since this request was sent, we don't want to continue
		//generating panels for the old space.
		if (DETAILS_JOB.sentSpaceId!=curSpaceId) {
			return;
		}
		$("#panelActions").after(child); //put the table after the panelActions fieldset

		var panelTableInitializer = getPanelTableInitializer(jobId, spaceId); 		

		panelArray[i]=$("#panel"+spaceId).dataTable(panelTableInitializer);
		$(".extLink").hide();
	}
	
	$(".viewSubspace").each(function() {
		$(this).click(function() {
			spaceId=$(this).parents("table.panel").attr("spaceId");
			openSpace(spaceId);	
		});
		
	});
	$(".panelField").expandable(true);
}

//
//  Initializes the DataTable objects
//
function initDataTables(){
	extendDataTableFunctions();
	//summary table
	
	if (isLocalJobPage) {
		$('[id$=solveTbl]').dataTable(DETAILS_JOB.solverTableInitializer);
	} else {
		summaryTable=$("#solveTbl").dataTable(DETAILS_JOB.solverTableInitializer);
	}
	
	$("#solveTbl").on("mousedown", "tr", function(){
		if (!$(this).hasClass("row_selected")) {
			$("#solveTbl").find(".second_selected").each(function(){
				$(this).removeClass("second_selected");
				$(this).removeClass("row_selected");

			});
			$("#solveTbl").find(".first_selected").each(function(){
				$(this).removeClass("first_selected");
				$(this).addClass("second_selected");

			});
			
			$(this).addClass("first_selected");
			$(this).addClass("row_selected");
		} else {
			$(this).removeClass("row_selected");
			$(this).removeClass("first_selected");
			$(this).removeClass("second_selected");

			$("#solveTbl").find(".second_selected").each(function(){
				$(this).removeClass("second_selected");
				$(this).removeClass("first_selected");

				$(this).addClass("first_selected");

			});
		}
		if ($("#solveTbl").find(".second_selected").size()>0) {
			$("#compareSolvers").show();
			
		} else {
			$("#compareSolvers").hide();

		}
	});
	
	// Job pairs table
	if (isLocalJobPage) {
		$('[id$=pairTbl]').dataTable(DETAILS_JOB.pairTableInitializer);
	} else {
		pairTable=$("#pairTbl").dataTable(DETAILS_JOB.pairTableInitializer);
	}
	
	setSortTable(pairTable);
	
	$("#pairTbl thead").click(function(){
		resetSortButtons();
	});
	
	$('#detailTbl').dataTable( {
		"sDom": 'rt<"bottom"f><"clear">',
		"aaSorting": [],
		"bPaginate": false,        
		"bSort": true        
	});
	
	$('#pairTbl tbody').on( "click", "a", function(event) {
		event.stopPropogation();
	});
	
	//Set up row click to send to pair details page
	if ( !DETAILS_JOB.isAnonymousPage ) {
		$("#pairTbl tbody").on("click", "tr",  function(){
			var pairId = $(this).find('input').val();
			window.location.assign(DETAILS_JOB.starexecUrl+"secure/details/pair.jsp?id=" + pairId);
		});
	}
	
	// Change the filter so that it only queries the server when the user stops typing
	$('#pairTbl').dataTable().fnFilterOnDoneTyping();
}


//
//Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
//
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
	
	
}

function getPairTableInitializer() {
	'use strict';
	var pairTableInitializer = {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
		"pagingType"    : "full_numbers"
    };

	if (!isLocalJobPage) {
		pairTableInitializer.sAjaxSource = starexecRoot+"services/jobs/";
		pairTableInitializer.sServerMethod = "POST";
		pairTableInitializer.bServerSide = true;
		pairTableInitializer.fnServerData = fnPaginationHandler;
	}
	return pairTableInitializer;
}

function getSolverTableInitializer() {
	'use strict';
	var solverTableInitializer = {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bSort"			: true,
        "bPaginate"		: true,
		"pagingType"    : "full_numbers"
    };

	if ( !isLocalJobPage ) {
        solverTableInitializer.sAjaxSource = starexecRoot+"services/jobs/";
        solverTableInitializer.sServerMethod = "POST";
        solverTableInitializer.fnServerData = fnStatsPaginationHandler;
	}

	return solverTableInitializer;
}

function fnShortStatsPaginationHandler(sSource, aoData, fnCallback) {
	$.post(  
			sSource+useWallclock+"/"+getSelectedStage(),
			aoData,
			function(nextDataTablePage){
				//if the user has clicked on a different space since this was called, we want those results, not these
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

function fnStatsPaginationHandler(sSource, aoData, fnCallback) {
	if (typeof curSpaceId=='undefined') {
		return;
	}
	var outSpaceId=curSpaceId;

	var postUrl = '';
	if ( DETAILS_JOB.isAnonymousPage ) {
		postUrl = sSource +"solvers/anonymousLink/pagination/"+outSpaceId+ "/" + getParameterByName("anonId") + 
				"/" + DETAILS_JOB.anonymizeNames+"/false/"+useWallclock+"/" + getSelectedStage();
	} else {
		postUrl = sSource +"solvers/pagination/"+outSpaceId+"/false/"+useWallclock+"/"+getSelectedStage();
	}

	$.post(  
			postUrl,
			aoData,
			function(nextDataTablePage){
				//if the user has clicked on a different space since this was called, we want those results, not these
				if (outSpaceId!=curSpaceId) {
					return;
				}
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					$("#solverSummaryField").show();
					$("#graphField").show();
					$("#statsErrorField").hide();
					fnCallback(nextDataTablePage);		
				}

			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}

//
//  Handles querying for pages in a given DataTable object
//  
//  @param sSource the "sAjaxSource" of the calling table
//  @param aoData the parameters of the DataTable object to send to the server
//  @param fnCallback the function that actually maps the returned page to the DataTable object
//
function fnPaginationHandler(sSource, aoData, fnCallback) {
	'use strict';
	if (typeof curSpaceId=='undefined') {
		return;
	}
	var outSpaceId=curSpaceId;
	if (sortOverride!=null) {
		aoData.push( { 'name': 'sort_by', 'value':getSelectedSort() } );
		aoData.push( { 'name': 'sort_dir', 'value':isASC() } );
	}

	var postUrl = null;
	if ( DETAILS_JOB.isAnonymousPage ) {
		postUrl = sSource + 'pairs/pagination/anonymousLink/' + DETAILS_JOB.anonymousLinkUuid  + '/' + outSpaceId +
				'/'+useWallclock+'/'+syncResults+'/'+getSelectedStage() + '/' + DETAILS_JOB.anonymizeNames;
	} else {
		postUrl = sSource + 'pairs/pagination/'+outSpaceId+'/'+useWallclock+'/'+syncResults+'/'+getSelectedStage();
	}

	$.post(  
			postUrl,
			aoData,
			function(nextDataTablePage){
				//do nothing if this is no longer the current request
				if (outSpaceId!=curSpaceId) {
					return;
				}
				var s=parseReturnCode(nextDataTablePage);
				if (s) {
					
					pairTable.fnProcessingIndicator(false);
					fnCallback(nextDataTablePage);
					$("#errorField").hide();
					if (pairTable.fnSettings().fnRecordsTotal()==0) {
						$("#pairTblField").hide();
					} else {
						$("#pairTblField").show();
					}
				} else {
					//if we weren't successful, we need to check to see if it was because there are too many pairs
					code=getStatusCode(nextDataTablePage);
					if (code==1) {
						$("#pairTblField").hide();
						$("#errorField").show();
					}
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating data table",5000);
	});
}

function popup(url) {
	'use strict';
	var win = window.open(url, '_blank');
	if (win) {
		// Browser allowed opening of popup.
		win.focus();
	}
}

function registerAnonymousLinkButtonEventHandler() {
	'use strict';
	$('#anonymousLink').unbind('click');
	$('#anonymousLink').click( function() {
		$('#dialog-confirm-anonymous-link').text(
				"Do you want the job, benchmark, solver, and configuration names to be hidden on the linked page?" );
		$('#dialog-confirm-anonymous-link').dialog({
			modal: true,
			width: 600,
			height: 200,
			buttons: {
				'everything': function() { 
					$(this).dialog('close');
					makeAnonymousLinkPost('job', jobId, 'all');
				},
				'everything except benchmarks': function() {
					$(this).dialog('close');
					makeAnonymousLinkPost( 'job', jobId, 'allButBench');
				},
				'nothing': function() {
					$(this).dialog('close');
					makeAnonymousLinkPost('job', jobId, 'none');
				}
			}
		});	
	});
}
