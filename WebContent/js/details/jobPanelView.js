var jobSpaceId; //stores the ID of the job space that is currently selected from the space viewer
var jobId; //the ID of the job being viewed
var panelArray=null;
$(document).ready(function(){
	jobId=$("#jobId").attr("value");	
	jobSpaceId=$("#spaceId").attr("value");
	//update the tables every 5 seconds
	setInterval(function() {
		for (i=0;i<panelArray.length;i++) {
			panelArray[i].fnReloadAjax(null,null,true);
		}
	},5000);
	
	initializePanels();
});

function getPanelTable(space) {
	spaceName=space.attr("name");
	spaceId=parseInt(space.attr("id"));
	
	table="<table id=panel"+spaceId+" spaceId=\""+spaceId+"\" class=\"panel\"><thead>" +
			"<tr class=\"panelHeader\"><th  colspan=\"4\">"+spaceName+"</th> </tr>" +
			"<tr><th class=\"solverHead\">solver</th><th class=\"configHead\">config</th> " +
			"<th class=\"solvedHead\">solved</th> <th class=\"timeHead\">time</th> </tr>" +
			"</thead>" +
			"<tbody></tbody> </table>";
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
		        "sAjaxSource"	: starexecRoot+"services/jobs/" + jobId+"/solvers/pagination/"+spaceId+"/true",
		        "sServerMethod" : "POST",
		        "fnServerData" : fnShortStatsPaginationHandler
		    });
		}
		$(".panelHeader").each(function() {
			$(this).click(function() {
				spaceId=$(this).parents("table.panel").attr("spaceId");
				window.location=starexecRoot+"secure/details/jobPanelView.jsp?jobid="+jobId+"&spaceid="+spaceId;
			});
			
		});
	});
	
}


	



/**
 * Adds fnProcessingIndicator and fnFilterOnDoneTyping to dataTables api
 */
function extendDataTableFunctions(){	
	//allows refreshing a table that is using client-side processing (for the summary table)
	//modified to work for the summary table-- this version of fnReloadAjax should not be used
	//anywhere else
	$.fn.dataTableExt.oApi.fnReloadAjax = function ( oSettings, sNewSource, fnCallback, bStandingRedraw)
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

function fnShortStatsPaginationHandler(sSource, aoData, fnCallback) {
	$.post(  
			sSource,
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
