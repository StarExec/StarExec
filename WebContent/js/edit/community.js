var defaultPPId = 0;
var leaderTable;


$(document).ready(function(){
	var id = $('#comId').val();
	
	leaderTable = $('#leaders').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	memberTable = $('#Members').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	

	
	$.get(  
			starexecRoot+"services/communities/details/" + id,  
			function(data){  			
				populateDetails(data);			
			},  
			"json"
		).error(function(){
			showMessage('error',"Internal error getting community details",5000);
		});
	
	initUI();
	attachFormValidation();
	attachWebsiteMonitor();
});

function populateDetails(jsonData) {
	var id = $('#comId').val();

	
	// Populate leaders table
	$('#leaderField legend').children('span:first-child').text(jsonData.leaders.length);
	leaderTable.fnClearTable();	
	$.each(jsonData.leaders, function(i, user) {
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="'+starexecRoot+'secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="'+starexecRoot+'images/external.png" /></a>';
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="'+starexecRoot+'images/external.png" /></a>';
		var deleteUser = '<input type="button" onclick="removeUser(' + user.id + ')" value="X"/>';
		var demoteUser = '<input type="button" onclick="demoteUser(' + user.id + ', ' + id + ')" value="Demote"/>';
		
		leaderTable.fnAddData([userLink, user.institution, emailLink, deleteUser, demoteUser]);

	});
	
	// Populate members table
	$('#memberField legend').children('span:first-child').text(jsonData.space.users.length);
	//memberTable.fnClearTable();	
	$.each(jsonData.space.users, function(i, user) {
		var hiddenUserId = '<input type="hidden" value="' + user.id + '" >';
		var fullName = user.firstName + ' ' + user.lastName;
		var userLink = '<a href="'+starexecRoot+'secure/details/user.jsp?id=' + user.id + '" target="blank">' + fullName + '<img class="extLink" src="'+starexecRoot+'images/external.png"/></a>' + hiddenUserId;
		var emailLink = '<a href="mailto:' + user.email + '">' + user.email + '<img class="extLink" src="'+starexecRoot+'images/external.png"/></a>';		
		var deleteUser = '<input type="button" onclick="removeUser(' + user.id + ', ' + id + ')" value="X"/>';
		var promoteUser = '<input type="button" onclick="promoteUser(' + user.id + ', ' + id + ')" value="Promote"/>';
		
		memberTable.fnAddData([userLink, user.institution, emailLink, deleteUser, promoteUser]);
		
	});
}

function removeUser(userid, id) {
	var idArray = new Array();
	idArray.push(userid);
	$.post(  
			starexecRoot+"services/remove/user/" + id,
			{selectedIds : idArray},
			function(returnCode) {
				switch (returnCode) {
					case 0:
						// Remove the rows from the page and update the table size in the legend
						//updateTable(memberTable);
						//$("#removeUser").fadeOut("fast");
						showMessage('success', "user(s) removed succesfully", 5000);
						break;
					case 1:
						showMessage('error', "an error occurred while processing your request; please try again", 5000);
					case 2:
						showMessage('error', "insufficient privileges; you must be a community leader to do that", 5000);
						break;
					case 3:
						showMessage('error', "you can not remove yourself from this space in that way, " +
								"instead use the 'leave' button to leave this community", 5000);
						break;
					case 4:
						showMessage('error', "you can not remove other leaders of this space", 5000);
						break;
				}
			},
			"json"
		).error(function(){
			showMessage('error',"Internal error removing user",5000);
		});
	setTimeout(function(){document.location.reload(true);}, 1000);
}

function promoteUser(userid, id) {
	var idArray = new Array();
	idArray.push(userid);		
	$.post(  
			starexecRoot+"services/makeLeader/" + id ,
			{selectedIds : idArray},
			function(returnCode) {
				switch (returnCode) {
					case 0:
						showMessage('success',"user(s) promoted successfully",5000);
						setTimeout(function(){document.location.reload(true);}, 1000);
						break;
					case 1:
						showMessage('error', "an error occurred while processing your request; please try again", 5000);
					case 2:
						showMessage('error', "insufficient privileges; you must be a community leader to do that", 5000);
						break;
					case 3:
						showMessage('error', "member is already a leader", 5000);
						break;
				}
			},
			"json"
		).error(function(){
			showMessage('error',"Internal error making user a leader",5000);
		});
	
}

function demoteUser(userId, id) {		
	$.post(  
			starexecRoot+"services/demoteLeader/" + id + "/" + userId ,
			function(returnCode) {
				switch (returnCode) {
					case 0:
						showMessage('success',"user(s) demoted successfully",5000);
						setTimeout(function(){document.location.reload(true);}, 1000);
						break;
					case 1:
						showMessage('error', "an error occurred while processing your request; please try again", 5000);
					case 2:
						showMessage('error', "insufficient privileges; you must be a community leader to do that", 5000);
						break;
					case 3:
						showMessage('error', "member is already not a leader", 5000);
						break;
				}
			},
			"json"
		).error(function(){
			showMessage('error',"Internal error demoting user from leader",5000);
		});
}

/**
 * Monitors the solver's "websites" and updates the server if the client adds/deletes any
 */
function attachWebsiteMonitor(){
	// Handles deleting an existing website
	$("#websiteTable").delegate(".delWebsite", "click", function(){
		var id = $(this).attr('id');
		var parent = $(this).parent().parent();
		var answer = confirm("are you sure you want to delete this website?");
		if (true == answer) {
			$.post(
					starexecRoot+"services/websites/delete/" + "space" + "/" + $('#comId').val() + "/" + id,
					function(returnData){
						if (returnData == 0) {
							parent.remove();
						} else {
							showMessage('error', "the website was not deleted due to an error; please try again", 5000);
						}
					},
					"json"
			).error(function(){
				showMessage('error',"Internal error updating community websites",5000);
			});
		}
	});
	
	// Handles adding a new website
	$("#addWebsite").click(function(){
		var name = $("#website_name").val();
		var url = $("#website_url").val();
		
		if(name.trim().length == 0) {
			showMessage('error', 'please enter a website name', 5000);
			return;
		} else if (url.indexOf("http://") != 0) {			
			showMessage('error', 'url must start with http://', 5000);
			return;
		} else if (url.trim().length <= 12) {
			showMessage('error', 'the given url is not long enough', 5000);
			return;
		}	
		
		var data = {name: name, url: url};
		$.post(
				starexecRoot+"services/website/add/space/" + $('#comId').val(),
				data,
				function(returnCode) {
			    	if(returnCode == '0') {
			    		$("#website_name").val("");
			    		$("#website_url").val("");
			    		$('#websites li').remove();
			    		refreshSpaceWebsites();
			    	} else {
			    		showMessage('error', "error: website not added. please try again", 5000);
			    	}
				},
				"json"
		);
		
	});
}

function initUI(){
	// Make forms editable
	editable("name");
	editable("desc");
	editable("CpuTimeout");
	editable("ClockTimeout");
	editable("MaxMem");
	// Add toggles for the "add new" buttons and hide them by default	
	$('#toggleWebsite').click(function() {
		$('#newWebsite').slideToggle('fast');
		togglePlusMinus(this);
	});	
	
	$('#toggleBenchType').click(function() {
		$('#newTypeTbl').slideToggle('fast');
		togglePlusMinus(this);
	});	
	
	$('#togglePostProcessor').click(function() {
		$('#newPostProcessTbl').slideToggle('fast');
		togglePlusMinus(this);
	});	
	
	$('#togglePreProcessor').click(function() {
		$('#newPreProcessTbl').slideToggle('fast');
		togglePlusMinus(this);
	});	
	
	$('#editPostProcess').click(function() {
		saveChanges($(this).children('option:selected').attr('value'), true, 'PostProcess', 0);
	});
	
	$('#editDependenciesEnabled').click(function() {
		saveChanges($(this).children('option:selected').attr('value'), true, 'DependenciesEnabled', 0);
	});
		
	// Set the selected post processor to be the default one
	defaultPPId = $('#editPostProcess').attr('default');
	$('#editPostProcess option[value=' + defaultPPId + ']').attr('selected', 'selected');
	
	defaultDepEnb=$('#editDependenciesEnabled').attr('default');
	$('#editDependenciesEnabled option[value=' + defaultDepEnb+']').attr('selected','selected');
	
	$('#newWebsite').hide();
	$('#newTypeTbl').hide();
	$('#newPostProcessTbl').hide();
	$('#newPreProcessTbl').hide();
	$('#dialog-confirm-delete').hide();
	
	$('#leaderField').expandable(false);
	$('#memberField').expandable(false);
	$('#websiteField').expandable(true);
	$('#benchmarkField').expandable(true);
	$('#settingsField').expandable(true);
	$('#processorField').expandable(true);
	$("#preProcessorField").expandable(true);
	$('#addType').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
	
	$('#addPostProcessor').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
	
	$('#addPreProcessor').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
	
	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
    }});	
}

/**
 * Attaches form validation to the pre/post processors and the benchmark type form
 */
function attachFormValidation(){
	// Re-validate the 'post-processor' and benchmark 'processor type' fields when they loses focus
	$("#typeFile").change(function(){
		 $("#typeFile").blur().focus(); 
    });
	$("#processorFile").change(function(){
		 $("#processorFile").blur().focus(); 
    });
	
	
	// Adds regular expression handling to JQuery validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	var formsToValidate = ['#addPreProcessorForm', '#newTypeForm'];
	
	$.each(formsToValidate, function(i, selector) {
		$(selector).validate({
			rules : {
				name : {
					required : true,
					maxlength: $("#procName").attr("length"),
					regex : getPrimNameRegex()
					
				},
				desc: {
					required : true,	
					maxlength: $("#procDesc").attr("length"),
					regex : getPrimDescRegex()
					
				},
				file: {
					required : true				
				}			
			},
			messages : {
				name : {
					required : "enter a processor name",
					maxlength : $("#procName").attr("length") + " characters maximum",
					regex : "invalid character(s)"
				},
				desc : {				
					required : "enter a processor description",
					maxlength : $("#procDesc").attr("length") + " characters maximum",	
					regex : "invalid character(s)"
				},
				file : {
					required : "choose a file"				
				}
			}		
		});	
	});	
}

/**
 * Refreshes the space's websites by clearing them from the DOM, querying for them, and
 * then re-populating the DOM with the new data
 */
function refreshSpaceWebsites(){
	location.reload();
	/*$.getJSON(starexecRoot+'services/websites/space/' + $("#comId").val(), processWebsiteData).error(function(){
		showMessage('error',"Internal error getting websites",5000);
	});*/
}

/**
 * Processes website data by adding a delete button to the HTML and inject that into the DOM
 
function processWebsiteData(jsonData) {
	// Injects the clickable delete button that's always present
	$('#websiteTable tr').remove();
	$.each(jsonData, function(i, site) {
		$('#websiteTable').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src=starexecRoot+"images/external.png"/></a></td><td><a class="delWebsite" id="' + site.id + '">delete</a></td></tr>');
	});
}*/


function editable(attribute) {
	$('#edit' + attribute).click(function(){
		var old = $(this).html();
		
		if(attribute == "desc") {
			$(this).after('<td><textarea>' + old + '</textarea>&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();
		} else if (attribute == "name"){
			$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();	
		} else if (attribute == "CpuTimeout"){
			$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();	
		} else if (attribute == "ClockTimeout"){
			$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();	
		} else if (attribute =="MaxMem") {
			$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();	

		}
		
		$('#save' + attribute).click(function(){saveChanges(this, true, attribute, old);});
		$('#cancel' + attribute).click(function(){saveChanges(this, false, attribute, old);});
		
		$('#save' + attribute).button({
			icons: {
				secondary: "ui-icon-check"
	    }});
		
		$('#cancel' + attribute).button({
			icons: {
				secondary: "ui-icon-close"
	    }});
	});
	
	$('#edit' + attribute).css('cursor', 'pointer');
} 

function saveChanges(obj, save, attr, old) {
	if (true == save) {
		var newVal;
		//since the description is in a textarea, we need to case switch on it to pull
		//from the correct object
		if (attr == 'desc') {
			newVal = $(obj).siblings('textarea:first').val();
			
			if (newVal.length>$('#descRow').attr('length')) {
				showMessage('error', $('#descRow').attr('length')+ " characters maximum",5000);
				return;
			}
		} else if (attr == 'name') {
			newVal = $(obj).siblings('input:first').val();
			if (newVal.length>$('#nameRow').attr('length')) {
				showMessage('error', $('#nameRow').attr('length')+ " characters maximum",5000);
				return;
			}
		} else if (attr == "PostProcess"){
			newVal = obj;
		} else if (attr == "CpuTimeout"){
			newVal = $(obj).siblings('input:first').val();
		} else if (attr == "ClockTimeout"){
			newVal = $(obj).siblings('input:first').val();
		} else if (attr == "DependenciesEnabled") {
			newVal=obj;
		} else if(attr = "MaxMem") {
			newVal = $(obj).siblings('input:first').val();
		}
		
		// Fixes 'session expired' bug that would occur if user inputed the empty String
		newVal = (newVal == "") ? "-1" : newVal;
		
		$.post(  
				starexecRoot+"services/edit/space/" + attr + "/" + getParameterByName("cid"),
				{val: newVal},
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		// Hide the input box and replace it with the table cell
			    		$(obj).parent().after('<td id="edit' + attr + '">' + newVal + '</td>').remove();
			    		// Make the value editable again
			    		editable(attr);
			    	} else {
			    		showMessage('error', "invalid characters; please try again", 5000);
			    		// Hide the input box and replace it with the table cell
			    		$(obj).parent().after('<td id="edit' + attr + '">' + old + '</td>').remove();
			    		// Make the value editable again
			    		editable(attr);
			    	}
			     },  
			     "json"  
		).error(function(){
			showMessage('error',"Internal error updating field",5000);
		});
	} else {
		// Hide the input box and replace it with the table cell
		$(obj).parent().after('<td id="edit' + attr + '">' + old + '</td>').remove();
		// Make the value editable again
		editable(attr);
	}
}

/**
 * Toggles the plus-minus state of the "+ add new" website button
 */
function togglePlusMinus(addSiteButton){
	if($(addSiteButton).text()[0] == "+"){
		$(addSiteButton).text("- add new");
	} else {
		$(addSiteButton).text("+ add new");
	}
}