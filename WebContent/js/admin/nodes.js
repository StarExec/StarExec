$(document).ready(function(){
    //$('.edit').editable('http://www.example.com/save.php');

	
	$( "#dialog-confirm-move" ).hide();

	
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";
	 
	 var id = -1;
	 
	// Initialize the jstree plugin for the community list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/cluster/queues",	// Where we will be getting json data from
				"data" : function (n) {  							
					return { id : n.attr ? n.attr("id") : -1, class:"jstree-drop" }; // What the default space id should be
				} 
			} 
		},
		"themes" : { 
			"theme" : "default", 					
			"dots" : false, 
			"icons" : true
		},			
		"types" : {				
			"max_depth" : -2,
			"max_children" : -2,					
			"valid_children" : [ "space" ],
			"types" : {						
				"active_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"inactive_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node"],
					"icon" : {
						"image" : starexecRoot + "images/jstree/off.png"
					}
				},
				"enabled_node" : {
					"valid_children" : [],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"disabled_node" : {
					"valid_children" : [],
					"icon" : {
						"image" : starexecRoot+"images/jstree/off.png"
					}
				}
			}
		},
		"plugins" : ["types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
	   id = data.rslt.obj.attr("id");
	   window['type'] = data.rslt.obj.attr("rel"); 
	   //updateActionId(id, type);
	   //getCommunityDetails(id);
	}).bind("move_node.jstree",function(event,data){
		$('#dialog-confirm-move-txt').text('are you sure you want to move node?');
		var nodeId = data.rslt.o[0].id;
		var queueId = data.rslt.o[0].id;


		$.post(  	    		
				starexecRoot+'services/cluster/move/node/'+ nodeId + '/' + queueId,
				function(returnCode) {
					log('AJAX response recieved with code ' + returnCode);
					if (returnCode==0) {
						showMessage('success', "node successfully moved", 2000);
					}
				},
				"json"
		).error(function(){
			showMessage('error',"Internal error moving node",5000);
		});	 									
		  
	}).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	
	initDataTables();

	
	$('#nodes tbody tr').live('click', function () {
		   $(this).toggleClass( 'row_selected' );
		} );
	
});


function initDataTables() {
	
	// Setup the DataTable objects
	nodeTable = $('#nodes').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"bFilter"		: false,
		"bInfo"			: false,
		"bPaginate"		: false,
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnDrawCallback": function( oSettings ) {
			for ( var i=0, iLen=oSettings.aoData.length ; i<iLen ; i++ ) {
				var columnNumber = nodeTable.fnGetData(0).length;
				var conflict = oSettings.aoData[i]._aData[columnNumber-1];
				var colorCSS = 'statusNeutral';
				if(conflict === 'clear') {
					colorCSS = 'statusClear';
				} else if(conflict === 'CONFLICT') {
					colorCSS = 'statusConflict';
				}
			oSettings.aoData[i].nTr.className += " "+ colorCSS;
			}
		},
		"fnServerData"	: fnPaginationHandler
	});
	nodeTable.makeEditable({
		"sUpdateURL": starexecRoot + "secure/update/nodeCount",
		"fnStartProcessingMode": function() {
			//alert("start");
			nodeTable.fnDraw();
		},
		"fnEndProcessingMode": function() {
			alert("end");
		},
		"fnOnEdited": function(status) {
			alert(status);
		},
	  });
}

function fnPaginationHandler(sSource, aoData, fnCallback) {

	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "nodes/dates/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#nodeExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				
				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

	
	
	
	