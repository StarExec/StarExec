var attrTable;
var qid=0;
// When the document is ready to be executed on
$(document).ready(function(){
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
	
	// DataTables setup
	attrTable = $('#details').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">',
        "bPaginate": false
    });	
});
 
/**
 * Populates the node details panel with information on the given node
 */
function getDetails(id, type) {
	var url = '';
	qid=id;
	if(type == 'queue') {
		url = starexecRoot+"services/cluster/queues/details/" + id;		
	} else if(type == 'enabled_node' || type == 'disabled_node') {
		url = starexecRoot+"services/cluster/nodes/details/" + id;			//This was originally "services/cluster/nodes/details/"
	} else  {
		showMessage('error',"Invalid node type",5000);
		return;
	}
	
	$('#loader').show();
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
		// Show the bar showing queue availability (we're dealing with a queue)
		$('#progressBar').show();
		$('#progressBar').progressBar(Math.floor((jsonData.slotsAvailable / jsonData.slotsTotal) * 100), 
			{ max: 100, 
			textFormat: 'percentage', 
			boxImage: starexecRoot+'images/progress_bar/progressbar.gif',
			barImage: {
				0:  starexecRoot+'images/progress_bar/progressbg_red.gif',
				40: starexecRoot+'images/progress_bar/progressbg_orange.gif',
				80: starexecRoot+'images/progress_bar/progressbg_green.gif'
			}});
		$('#activeStatus').hide();
		
		attrTable.fnClearTable();
		for (var key in jsonData.jobPairs) {
			attrTable.fnAddData([jsonData.jobPairs[key]]);
		}
	} else {
		// We're not showing a queue, hide the bar showing queue availability and show active status
		$('#progressBar').hide();
		$('#activeStatus').show();
		
		if(jsonData.status == 'ACTIVE') {
			$('#activeStatus').text('[ACTIVE]');
			$('#activeStatus').css('color', '#008d03');
		} else {
			$('#activeStatus').text('[INACTIVE]');
			$('#activeStatus').css('color', '#ae0000');
		}
		attrTable.fnClearTable();
		for(var key in jsonData.attributes){
			attrTable.fnAddData([key, jsonData.attributes[key]]);            
	    }
	}
	
	// Done loading, hide the loader
	$('#loader').hide();
}