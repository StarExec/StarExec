var attrTable;

// When the document is ready to be executed on
$(document).ready(function(){
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = "/starexec/css/jstree/";

	 // Initialize the jstree plugin for the explorer list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/cluster/nodes",	// Where we will be getting json data from 
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
			"valid_children" : [ "worker_node" ],
			"types" : {						
				"space" : {
					"valid_children" : [ "worker_node" ],
					"icon" : {
						"image" : "/starexec/images/tree_level.png"
					}
				}
			}
		},
		"plugins" : [ "types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
        id = data.rslt.obj.attr("id");        
        getNodeDetails(id);
    }).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title
	
	// DataTables setup
	attrTable = $('#details').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',
        "bPaginate": false
    });	
});
 
/**
 * Populates the node details panel with information on the given node
 */
function getNodeDetails(id) {
	$('#loader').show();
	$.get(  
		"/starexec/services/cluster/nodes/details/" + id,  
		function(data){  			
			populateAttributes(data);			
		},  
		"json"
	).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
}

/**
 * Takes in a json  response and populates the details panel with information
 * @param jsonData the json data to populate the details page with
 */
function populateAttributes(jsonData) {	
	// Populate node details	
	$('#workerName').text(jsonData.name);
	attrTable.fnClearTable();	
	for(var key in jsonData.attributes){
		attrTable.fnAddData([key, jsonData.attributes[key]]);            
    }
		
	// Done loading, hide the loader
	$('#loader').hide();
}