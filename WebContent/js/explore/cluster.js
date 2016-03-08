var jobPairTable;
var qid=0;
// When the document is ready to be executed on
$(document).ready(function(){
	initDataTables();
	//Set up row click to send to pair details page
	$('#details tbody').on( "click", "a", function(event) {
		event.stopPropogation();
	});
	$("#details tbody").on( "click", "tr", function(){
		if (jobPairTable.fnTotalRecords()>0) {
			var pairId = $(this).find('input').val();
			window.location.assign(starexecRoot+"secure/details/pair.jsp?id=" + pairId);
		}
		
	});
	// Build left-hand side of page (cluster explorer)
	 initClusterExplorer();
	 loadQstatOutput();
	 
	 $("#refreshQstat").button( {
		 icons: {
		 	primary: "ui-icon-refresh"
	 	}
	 });
	 $("#refreshLoads").button( {
		 icons: {
		 	primary: "ui-icon-refresh"
	 	}
	 });
	 $("#refreshQstat").click(function() {
		 loadQstatOutput();
	 });
	 $("#refreshLoads").click(function() {
		 loadQueueLoads();
	 });
	 
	 $("#qstatField").expandable(true);
	 $("#loadsField").expandable(true);

	 
	 setInterval(function() {
		 jobPairTable.fnDraw(false);
	 }, 10000);
});
	 
function initClusterExplorer() {
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";

	 $("#exploreList").bind("loaded.jstree", function(e, data) {
		 // Register a callback for when the jstree has finished loading
		addNodeCountsToTree();
	 }).jstree({  
		 // Initialize the jstree plugin for the explorer list
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
				"active_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/on.png"
					}
				},
				"inactive_queue" : {
					"valid_children" : [ "enabled_node", "disabled_node" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/off.png"
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
		getDetails(id,data.rslt.obj.attr("rel"));
	}).on( "click", "a", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
}


function initDataTables() {
	jobPairTable = $('#details').dataTable( {
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bServerSide"	: true,
        "bFilter"		: false,
        "sAjaxSource"	: starexecRoot+"services/cluster/",
        "sServerMethod" : "POST",
        "fnServerData"	: fnPaginationHandler 
    });
}

function fnPaginationHandler(sSource, aoData, fnCallback) {		
	var id = $('#exploreList').find('.jstree-clicked').parent().attr("id");
	//If we can't find the id of the queue/node from the DOM, get it from the cookie instead
	if (id == null || typeof id == 'undefined') {
		id = $.cookie("jstree_select");
		// If we also can't find the cookie, then just set the space selected to be the root space
		if (id == null || typeof id == 'undefined') {
			$('#exploreList').jstree('select_node', '#1', true);
			id = 1;
		} else {
			id = id[1];
		}
	}

	//In case the paginate happens before type is set
	if (typeof type == 'undefined') {
		window['type'] = 'queues';
	}
	//we have no pagination for inactive queues
		$.get(  
				sSource + window['type'] + "/" + id + "/pagination",
				aoData,
				function(nextDataTablePage){
					s=parseReturnCode(nextDataTablePage);
					if (s) {
						fnCallback(nextDataTablePage);
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
	loadQueueLoads();
	jobPairTable.fnClearTable();	//immediately get rid of the current data, which makes it look more responsive
	if(type == 'active_queue' || type == 'inactive_queue') {
		url = starexecRoot+"services/cluster/queues/details/" + id;	
		window['type'] = 'queues';
	} else if(type == 'enabled_node' || type == 'disabled_node') {
		url = starexecRoot+"services/cluster/nodes/details/" + id;
		window['type'] = 'nodes';
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

function loadQstatOutput() {
	$.get(  
			starexecRoot+"services/cluster/qstat",
			{},
			function(data){
				$("#qstatOutput").val(data);

			},  
			"text"
	);
}

function loadQueueLoads() {
	$.get(  
			starexecRoot+"services/cluster/loads/"+qid,
			{},
			function(data){
				$("#loadOutput").val(data);

			},  
			"text"
	);
}

/**
 * Takes in a json  response and populates the details panel with information
 * @param jsonData the json data to populate the details page with
 */
function populateAttributes(jsonData) {	
	// Populate node details	
	$('#workerName').text(jsonData.name.split('.')[0]);
	$('#queueID').text("id = "+qid);

	if(jsonData.cpuTimeout != null) {
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
