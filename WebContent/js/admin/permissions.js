var space_id;

$(document).ready(function(){
	
	$("#editPermissions").button({
		icons: {
			primary: "ui-icon-pencil"
		}
    });
	
	$("#dialog-confirm-update").hide();
	
	$("#editPermissions").hide();
	
	function getUrlVars() {
	    var vars = {};
	    var parts = window.location.href.replace(/[?&]+([^=&]+)=([^&]*)/gi, function(m,key,value) {
	        vars[key] = value;
	    });
	    return vars;
	}

	var id = getUrlVars()["id"];
		
	// Set the path to the css theme fr the jstree plugin
	 $.jstree._themes = starexecRoot+"css/jstree/";
	 	 
	// Initialize the jstree plugin for the community list
	jQuery("#exploreList").jstree({  
		"json_data" : { 
			"ajax" : { 
				"url" : starexecRoot+"services/space/userAssoc/"+id	// Where we will be getting json data from 				
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
				"space" : {
					"valid_children" : [ "space" ],
					"icon" : {
						"image" : starexecRoot+"images/jstree/users.png"
					}
				}
			}
		},
		"plugins" : ["types", "themes", "json_data", "ui", "cookies"] ,
		"core" : { animation : 200 }
	}).bind("select_node.jstree", function (event, data) {
		// When a node is clicked, get its ID and display the info in the details pane		
	   space_id = data.rslt.obj.attr("id");
	   //updateActionId(id);
	   getPermissionDetails(id, space_id);
       initUI(id, space_id);

	}).delegate("a", "click", function (event, data) { event.preventDefault(); });	// This just disable's links in the node title

	
});

function getPermissionDetails(id, space_id) {	
	$.get(  
		starexecRoot+"services/permissions/details/" + id + "/" + space_id,  
		function(data){  			
			populateDetails(data);			
		},  
		"json"
	).error(function(){
		showMessage('error',"Internal error getting permission details",5000);
	});
}

function populateDetails(data) {
	var addSolver = data.perm.addSolver;
	var addBench = data.perm.addBenchmark;
	var addUser = data.perm.addUser;
	var addSpace = data.perm.addSpace;
	var addJob = data.perm.addJob;
	var removeSolver = data.perm.removeSolver;
	var removeBench = data.perm.removeBench;
	var removeUser = data.perm.removeUser;
	var removeSpace = data.perm.removeSpace;
	var removeJob = data.perm.removeJob;
	var isLeader = data.perm.isLeader;
	
	checkBoxes("addSolver", addSolver);
	checkBoxes("addBench", addBench);
	checkBoxes("addUser", addUser);
	checkBoxes("addSpace", addSpace);
	checkBoxes("addJob", addJob);
	checkBoxes("removeSolver", removeSolver);
	checkBoxes("removeBench", removeBench);
	checkBoxes("removeUser", removeUser);
	checkBoxes("removeJob", removeJob);
	checkBoxes("removeSpace", removeSpace);
	checkBoxes("isLeader", isLeader);
	
}

function checkBoxes(name, value) {
	if (value == true) {
		$("#" + name).attr('checked', 'checked');
	} else {
		$("#" + name).removeAttr('checked');

	}
}


function initUI(userId, spaceId){
	
	$("#editPermissions").show();
	
	$("#editPermissions").click(function(){
		$("#dialog-confirm-update-txt").text("are you sure you want to edit this user's permissions for this space?");
		
		$("#dialog-confirm-update").dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-confirm-update').dialog('close');
					var data = 
					{		addBench	: $("#addBench").is(':checked'),
							addJob		: $("#addJob").is(':checked'),
							addSolver	: $("#addSolver").is(':checked'),
							addSpace	: $("#addSpace").is(':checked'),
							addUser		: $("#addUser").is(':checked'),
							removeBench	: $("#removeBench").is(':checked'),
							removeJob	: $("#removeJob").is(':checked'),
							removeSolver: $("#removeSolver").is(':checked'),
							removeSpace	: $("#removeSpace").is(':checked'),
							removeUser	: $("#removeUser").is(':checked'),
							isLeader 	: $("#isLeader").is(':checked'),
					};
					// Pass data to server via AJAX
					$.post(
							starexecRoot+"services/space/" + spaceId + "/edit/perm/" + userId,
							data,
							function(returnCode) {
								switch (returnCode) {
									case 0:
										showMessage('success', "user's permission were successfuly updated", 5000);
										//window.location = starexecRoot+'secure/admin/permissions.jsp?id=' + userId;
										break;
									case 1:
										showMessage('error', "space details were not updated; please try again", 5000);
										break;
									case 2:
										showMessage('error', "only a leader of this space can modify its details", 5000);
										break;
									case 7:
										showMessage('error', "names must be unique among subspaces. It is possible a subspace you do not have permission to see shares the same name",5000);
										break;
									default:
										showMessage('error', "invalid parameters", 5000);
										break;
								}
							},
							"json"
					);
			},
			"cancel": function() {
				log('user canceled StarExec restart');
				$(this).dialog("close");
			}
			}
		});
	});
}


