var jobPairTable;
var qid=0;
// When the document is ready to be executed on
$(document).ready(function(){
	 
	//Set up row click to send to pair details page
	$('#details tbody').delegate("a", "click", function(event) {
		event.stopPropogation();
	});
	$("#details tbody").delegate("tr", "click", function(){
		var pairId = $(this).find('input').val();
		window.location.assign(starexecRoot+"secure/details/pair.jsp?id=" + pairId);
	});
	
	// Build left-hand side of page (cluster explorer)
	 initClusterExplorer();

	 initDataTables();
	 
	 setInterval(function() {
		 jobPairTable.fnDraw(false);
	 }, 10000);

	 
});
	 
function initClusterExplorer() {
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";

	 // Initialize the jstree plugin for the explorer list
		jQuery("#exploreList").jstree({  
			"json_data" : { 
				"ajax" : { 
					"url" : starexecRoot+"services/cluster/queues",	// Where we will be getting json data from 
					"data" : function (n) {  							
						return { id : n.attr ? n.attr("id") : -1 }; // What the default space id should be
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
				"valid_children" : [ "queue" ],
				"types" : {						
					"queue" : {
						"valid_children" : [ "enabled_node", "disabled_node" ],
						"icon" : {
							"image" : starexecRoot+"images/jstree/cogs.png"
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
			"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
			"core" : { animation : 200 }
		}).bind("select_node.jstree", function (event, data) {
			// When a node is clicked, get its ID and display the info in the details pane		
			id = data.rslt.obj.attr("id");
	        type = data.rslt.obj.attr("rel");   
	        getDetails(id, type);
	    }).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
}

function initDataTables() {
	jobPairTable = $('#details').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": 10,
        "bServerSide"	: true,
        "bFilter"		: false,
        "sAjaxSource"	: starexecRoot+"services/cluster/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
}

function fnPaginationHandler(sSource, aoData, fnCallback) {		
	var type = $('.jstree-clicked').text();
	type = type.split('.')[1];	
	
	var id = $('#exploreList').find('.jstree-clicked').parent().attr("id");
	//If we can't find the id of the queue/node from the DOM, get it from the cookie instead
	if (id == null || id == undefined) {
		id = $.cookie("jstree_select");
		// If we also can't find the cookie, then just set the space selected to be the root space
		if (id == null || id == undefined) {
			$('#exploreList').jstree('select_node', '#1', true);
			id = 1;
		} else {
			id = id[1];
		}
	}
			
	if(type == 'q') {
		type = 'queues';
	} else if(type == undefined) {
		type = 'nodes';
	} else  {
		showMessage('error',"Invalid node type",5000);
		return;
	}
	

	$.get(  
			sSource + type + "/" + id + "/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
					case 1:
						showMessage('error', "failed to get the next page of results; please try again", 5000);
						break;
					case 2:
						showMessage('error', "you do not have sufficient permissions to view primitives for this user", 5000);
						break;
					default:
 						fnCallback(nextDataTablePage);
 						break;
				}
			},  
			"json"
	).error(function(){
		showMessage('error',"Internal error populating table",5000);
	});
}
 
/**
 * Populates the node details panel with information on the given node
 */
function getDetails(id, type) {
	var url = '';
	qid=id;
	if(type == 'queue') {
		url = starexecRoot+"services/cluster/queues/details/" + id;	
	} else if(type == 'enabled_node' || type == 'disabled_node') {
		url = starexecRoot+"services/cluster/nodes/details/" + id;
	} else  {
		showMessage('error',"Invalid node type",5000);
		return;
	}
	
	$('#loader').show();
	
	jobPairTable.fnDraw();
	
	$.get(  
		url,  
		function(data){  			
			populateAttributes(data);			
		},  
		"json"
	).error(function(){
		showMessage('error',"Internal error getting node details",5000);
	});
}



/**
 * Takes in a json  response and populates the details panel with information
 * @param jsonData the json data to populate the details page with
 */
function populateAttributes(jsonData) {	
	// Populate node details	
	$('#workerName').text(jsonData.name.split('.')[0]);
	$('#queueID').text("id = "+qid);

	if(jsonData.slotsTotal != null) {
		//We're dealing with a queue
		$('#activeStatus').hide();

	} else {
		//It is a node
		$('#activeStatus').show();
		
		if(jsonData.status == 'ACTIVE') {
			$('#activeStatus').text('[ACTIVE]');
			$('#activeStatus').css('color', '#008d03');
		} else {
			$('#activeStatus').text('[INACTIVE]');
			$('#activeStatus').css('color', '#ae0000');
		}
	}
	
	// Done loading, hide the loader
	$('#loader').hide();
}