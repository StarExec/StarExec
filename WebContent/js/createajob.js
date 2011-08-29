$(function(){
	// Initialize jsTree on the benchList element
	jQuery("#benchList").jstree({  
		// Exchange data using JSON via AJAX
		"json_data" : { 
			"ajax" : { 
				// The service URL to get the benchmarks from
				"url" : "/starexec/services/level/bench", 
				// Set the ID attr as the identifier
				"data" : function (n) {  							
					return { id : n.attr ? n.attr("id") : -1 };  
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
			"valid_children" : [ "level" ],
			"types" : {						
				// Specify the 'level' type (directory)
				"level" : {
					"valid_children" : [ "bench", "level" ],
					"icon" : {
						"image" : "/starexec/images/tree_level.png"
					}
				},			
				// Specify the benchmark type
				"bench" : {							
					"valid_children" : "none",
					"icon" : {
						"image" : "/starexec/images/tree_bench2.png"
					}
				}
			}
		},
		"plugins" : [ "types", "themes", "json_data", "checkbox"] ,
		"core" : { animation : 200 }
	});
	
	// Initialize the solver list with jsTree
	jQuery("#solverList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/solvers", 
				"data" : function (n) {  							
					return { id : n.attr ? n.attr("id") : -1 };  
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
			"valid_children" : [ "solver" ],
			"types" : {						
				"solver" : {
					"valid_children" : "config",
					"icon" : {
						"image" : "/starexec/images/tree_solver.png"
					}
				},
				"config" : {
					"valid_children" : "none",
					"icon" : {
						"image" : "/starexec/images/tree_config.png"
					}
				}				
			}
		},
		"plugins" : [ "types", "themes", "json_data", "checkbox"] ,
		"core" : { animation : 200 }
	});
});

function doSubmit(){		
	var lvlList = [];
	var benchList = [];
	var solverList = [];
	var configList = [];
	
	$("#benchList").jstree("get_checked", false, false).each(function(i, data){
		// For each item checked in the benchmark tree...
		
		// Get the type
		var type = $(data).attr("rel");
		
		// And add it to the appropriate list
		if(type == "level") {
			lvlList.push(data.id);
		} else if (type == "bench") {
			benchList.push(data.id);
		}
    });
	
	$("#solverList").jstree("get_checked", false, false).each(function(i, data){
		// For each solver in the list...
		
		// Get the type
		var type = $(data).attr("rel");
		
		// And add it to the appropriate list
		if(type == "solver") {			
			solverList.push(data.id);
		} else if (type == "config") {
			configList.push(data.id);
		}
    });
		
	// Add the action of the form to be the URL with the appropriate params
	$('#jobForm').attr('action', 'SubmitJob?bench=' + benchList.join() + '&level=' + lvlList.join() + '&solver=' + solverList.join() + '&config=' + configList.join());	
	$('#btnSubmit').text('Uploading');
	$('#btnSubmit').attr('disabled', 'disabled');
	$('form').submit();
}