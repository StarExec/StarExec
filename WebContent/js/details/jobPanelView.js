var jobSpaceId; //stores the ID of the job space that is currently selected from the space viewer
var jobId; //the ID of the job being viewed
var panelArray=null;
var useWallclock=true;
$(document).ready(function(){
	jobId=$("#jobId").attr("value");	
	jobSpaceId=$("#spaceId").attr("value");

	//update the tables every 5 seconds
	setInterval(function() {
		refreshPanels();
	},5000);
	initUI();
	initializePanels();
});

function setTimeButtonText(){
	if (useWallclock){
		$("#changeTime .ui-button-text").html("use CPU time");
	} else {
		$("#changeTime .ui-button-text").html("use wall time");
	}
}

function refreshPanels(){
	for (i=0;i<panelArray.length;i++) {
		panelArray[i].api().ajax.reload(null,false);
	}
}


function initUI() {
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
	$("#changeTime").button({
		icons: {
			primary: "ui-icon-refresh"
		}
	
	});
	$("#changeTime").click(function() {
		useWallclock=!useWallclock;
		setTimeButtonText();
		refreshPanels();
	});
	$("#pageHeader").hide();
	$("#pageFooter").hide();
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
}

function getPanelTable(space) {
	spaceName=space.attr("name");
	spaceId=parseInt(space.attr("id"));
	
	table="<fieldset class=\"panelField\">" +
			"<legend class=\"panelHeader\">"+spaceName+"</legend>" +
			"<table id=panel"+spaceId+" spaceId=\""+spaceId+"\" class=\"panel\"><thead>" +
					"<tr class=\"viewSubspace\"><th colspan=\"4\" >Go To Subspace</th></tr>" +
			"<tr><th class=\"solverHead\">solver</th><th class=\"configHead\">config</th> " +
			"<th class=\"solvedHead\">solved</th> <th class=\"timeHead\">time</th> </tr>" +
			"</thead>" +
			"<tbody></tbody> </table></fieldset>";
	return table;
	
}

function initializePanels() {
	$.getJSON(starexecRoot+"services/space/" +jobId+ "/jobspaces?id="+jobSpaceId,function(spaces) {
		panelArray=new Array();		
		for (i=0;i<spaces.length;i++) {
			
			space=$(spaces[i]);
			spaceName=space.attr("name");
			spaceId=parseInt(space.attr("id"));
			
			child=getPanelTable(space);
			$("#subspaceSummaryField").append(child);
			panelArray[i]=$("#panel"+spaceId).dataTable({
		        "sDom"			: 'rt<"clear">',
		        "iDisplayStart"	: 0,
		        "iDisplayLength": 1000, // make sure we show every entry
		        "sAjaxSource"	: starexecRoot+"services/jobs/" + jobId+"/solvers/pagination/"+spaceId+"/true/",
		        "sServerMethod" : "POST",
		        "fnServerData" : fnShortStatsPaginationHandler
		    });
		}
		$(".viewSubspace").each(function() {
			$(this).click(function() {
				spaceId=$(this).parents("table.panel").attr("spaceId");
				window.location=starexecRoot+"secure/details/jobPanelView.jsp?jobid="+jobId+"&spaceid="+spaceId;
			});
			
		});
		$(".panelField").expandable();
	});
	
}

function fnShortStatsPaginationHandler(sSource, aoData, fnCallback) {
	$.post(  
			sSource+useWallclock,
			aoData,
			function(nextDataTablePage){
				//if the user has clicked on a different space since this was called, we want those results, not these
				
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
