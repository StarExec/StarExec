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
		"crrm" : {
	        move : {
	            "check_move" : function (m) {

	                var p = this._get_parent(m.o);
	                //alert(m.r.attr("rel"));	// This returns the id of the queue it currently belongs to
	                if(!p) 
	                    return false;
	                if(m.cr===-1)
	                    return false;
	                //Only allow drops onto active_queue
	               if(m.r.attr("rel") != "active_queue")
	            	   return false;
	               //can't drop into the queue it already belongs to
	               if(m.r.attr("id") == p.attr("id"))
	            	   return false;
	                return true;        
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
		"plugins" : ["types", "themes", "json_data", "ui", "cookies", "dnd", "crrm"] ,
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
				var conflict = oSettings.aoData[i]._aData[5];
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
	
	/* Apply the jEditable handlers to the table */
	//$(nodeTable.fnGetNodes()).editable( 'http://localhost:8080/starexec/secure/admin/nodes.jsp');
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
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
//					showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#nodeExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				
				// Make the table that was just populated draggable too
				initDraggable('#nodes');

				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}


/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 * @author Tyler Jensen & Todd Elvers
 */
function initDraggable(table) {
	var rows = $('#nodes').children('tbody').children('tr');
	
	// Make each queue in the explorer list be a droppable target; keep in initDraggable()
	// as it only allows queues to be droppable and not the nodes
	$('#exploreList').find('a').droppable( {
		drop		: onQueueDrop,
		tolerance	: 'pointer',	// Use the pointer to determine drop position instead of the middle of the drag clone element
        
		activeClass	: 'active'		// Class applied to the space element when something is being dragged
	});

	
	// Using jQuery UI, make the first column in each row draggable
	rows.draggable({
		cursorAt: { cursor: 'move', left: -1, bottom: -1},	// Set the cursor to the move icon and make it start in the corner of the helper		
		containment: 'document',							// Allow the element to be dragged anywhere in the document
		distance: 20,										// Only trigger a drag when the distanced dragged is > 20 pixels
		scroll: true,										// Scroll with the page as the item is dragged if needed
		helper: getDragClone,								// The method that returns the 'cloned' element that is dragged
		start: onDragStart,									// Method called when the dragging begins
		stop: onDragStop									// Method called when the dragging ends
	});
	

	// Set the JQuery variables used during the drag/drop process
	$.each(rows, function(i, row){
		$(row).data("id", $(row).children('td:first-child').children('input').val());
		$(row).data("type", $(row).children('td:first-child').children('input').attr('prim'));

		// if it is comment then do not display the first field
		if($(row).data('type') !== undefined && $(row).data('type')[0] == 'c'){
			$(row).data("name","this comment");
		}else{
			$(row).data("name", $(row).children('td:first-child').children('a').text());
		}
	});	

	log($(table).attr('id') + ' table initialized as draggable');
}

/**
 * Returns the html of an element that is dragged along with the mouse when an item is dragged on the page
 * @author Tyler Jensen
 */
function getDragClone(event) {
	var src = $(event.currentTarget);
	if(false == $(src).hasClass('row_selected')){ //change
		$(src).addClass('row_selected');
	}
	var ids = getSelectedRows($(src).parents('table:first'));
	//var ids = 1;

	var txtDisplay = $(src).children(':first-child').text();
	var icon = 'ui-icon ';	

	var primType = "node";
	log(src);


	if(ids.length > 1) {
		txtDisplay = ids.length + ' ' + primType + 's';
	}

	// Change the drag icon based on what the type of object being dragged is
	switch(primType[0]){
	case 'u':
		icon += 'ui-icon-person';
		break;
	default:
		icon += 'ui-icon-newwin';
	break;
	}

	// Return a styled div with the name of the element that was originally dragged
	return '<div class="dragClone"><span class="' + icon + '"></span>' + txtDisplay + '</div>';
	alert("found drag clone");
}

/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	log('drag started');
}

/**
 * Called when there is no longer anything being dragged
 */
function onDragStop(event, ui) {
	log('drag stopped');
}


/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 * 
 * @param dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
 * @author Todd Elvers
 */
function getSelectedRows(dataTable){
	var idArray = new Array();
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		idArray.push($(this).children('td:first').children('input').val());
	});
	return idArray;
}


function onQueueDrop(event, ui) {
	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));

	// Get the destination space id and name
	var destQueue = $(event.target).parent().attr('id');
	var destName = $(event.target).text();

	log(ids.length + ' rows dropped onto ' + destName);
	if(ids.length < 2) {
		$('#dialog-confirm-move-txt').text('do you want to move node ' + ui.draggable.data('name') + ' to' + destName +'?');
	} else {
		$('#dialog-confirm-move-txt').text('do you want to move the ' + ids.length + ' selected nodes to' + destName +'?');
	}
	
	$('#dialog-confirm-move').dialog({
		modal: true,
		width: 380,
		height: 165,
		buttons: {
			'ok': function() {
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-move').dialog('close');
				// Make the request to the server	
				$.post(  	    		
						starexecRoot+'services/cluster/' + destQueue+ '/move/node',
						{selectedIds : ids},	
						function(returnCode) {
							log('AJAX response recieved with code ' + returnCode);
							if (returnCode==0) {
								if(ids.length > 1) {								
									showMessage('success', ids.length + ' ' + 'jobs successfully linked in' + destName, 2000);
								} else {					    		
									showMessage('success', 'job successfully copied to' + destName, 2000);	
								}
							}else {
								processCopyErrorCode(returnCode, "jobs",destName);
							}
						},
						"json"
				).error(function(){
					showMessage('error',"Internal error copying jobs",5000);
				});	 									
			},
			"cancel": function() {
				log('user canceled move action');
				$(this).dialog("close");
			}
		}		
	});		
}

	
	
	
	