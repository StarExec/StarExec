$(function(){
	jQuery("#levels").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : "/starexec/services/levels/sublevels",
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
					"valid_children" : [ "level" ],
					"icon" : {
						"image" : "/starexec/images/tree_level.png"
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
	
	$("#levels").jstree("get_checked", false,false).each(function(i, data){		
		lvlList.push(data.id);		
    });
			
	$('#upForm').attr('action', 'UploadSolver?lvl=' + lvlList.join() + '&n=' + $('#sName').val());	// Set the form to submit to the UploadSolver servlet with the selected values
	
	if(lvlList.length){								// If we had at least one supported level selected...
		$('#btnSubmit').text('Uploading');
		$('#btnSubmit').attr('disabled', 'disabled');
		$('form').submit();
	} else {											// Else show an error and return false
		alert("The solver must support at least one division!");		
	}
}