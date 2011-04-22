$(function(){
	jQuery("#benchList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/level/bench", 
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
				"level" : {
					// can have files and other folders inside of it, but NOT `drive` nodes
					"valid_children" : [ "bench", "level" ],
					"icon" : {
						"image" : "/starexec/images/tree_level.png"
					}
				},						
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
	
	
	jQuery("#solverList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/solvers/all", 
				"data" : function (n) {  							
					return { id : -1 };  
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
					// can have files and other folders inside of it, but NOT `drive` nodes
					"valid_children" : "none",
					"icon" : {
						"image" : "/starexec/images/tree_solver.png"
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
	
	$("#benchList").jstree("get_checked", false,false).each(function(i, data){
		var type = $(data).attr("rel");
		
		if(type == "level")			
			lvlList.push(data.id);
		else if (type == "bench")
			benchList.push(data.id);		
    });
	
	$('jobForm').attr('action', "SubmitJob?bench=" + benchList.join(',') + "&level=" + lvlList.join(','));
	$('jobForm').submit();
}