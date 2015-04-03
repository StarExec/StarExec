var userId;

$(document).ready(function(){
	initUI();
	attachFormValidation();
	attachPasswordMonitor();
	attachWebsiteMonitor();
	userId=$("#infoTable").attr("uid");
});


/**
 * Attaches form validation to the 'change password' fields
 */
function attachFormValidation(){
	// Hide the password strength meter initially and display it when a password's
	// strength has been calculated
	$.validator.passwordStrengthMeter("#pwd-meter");
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#changePassForm").submit(function(e){
		e.preventDefault();
	});
	
	// Form validation rules/messages
	$("#changePassForm").validate({
		rules : {
			current_pass : {
				required : true
			},
			confirm_pass : {
				required : true,
				equalTo : "#password"
			}
		},
		messages : {
			current_pass : {
				required : "enter old password"
			},
			confirm_pass : {
				required : "re-enter password",
				equalTo : "does not match"
			}
		},
		// Don't display an error for passwords, the password strength meter takes care of that
		errorPlacement : function(error, element) {
			if($(element).attr("id") != "password"){
				error.insertAfter(element);
			}
		}
	});
}

/**
 * Initialize the DataTable objects for this page
 */
function initDataTables(){
	$('#personal').dataTable( {
        "sDom": 'rt<"bottom"><"clear">',
        "aaSorting": [],
        "bPaginate": false,        
        "bSort": true        
    });
}

/**
 * Initializes the user-interface
 */
function initUI(){
	$("#dialog-confirm-delete").hide();
	$("#dialog-createSettingsProfile").hide();

	initDataTables();
	initButtonIcons();
	
	// Collapse all fieldsets on page load except for the one containing the client's information
	$('fieldset:not(:first, #settingActions)').expandable(true);
	
	// Setup "+ add new" & "- add new" animation
	$('#toggleWebsite').click(function() {
		$('#new_website').slideToggle('fast');
		togglePlusMinus(this);
	});	
	$('#new_website').hide();
	
	// Allow the client to edit their first name, last name, and institution name
	editable("firstname");
	editable("lastname");
	editable("institution");
	editable("diskquota");
	editable("pagesize");
	// If the client's picture is clicked on, pop it up in a JQuery modal window
	$('#showPicture').click(function(event){
		popUp($(this).attr('enlarge'));
	});
	
	
	$("button").button();
	
	
	$("#saveProfile").click(function() {
		$.post(  
				starexecRoot+"secure/add/profile",
				{postp: $("#editPostProcess").val(), prep: $("#editPreProcess").val(), benchp: $("#editBenchProcess").val(),
					solver: $("#solver").val(), name: $("#settingName").val(), cpu: $("#cpuTimeout").val(),
					wall: $("#wallclockTimeout").val(), dep: $("#editDependenciesEnabled").val(),
					bench: $("#benchmark").val(), mem: $("#maxMem").val(), settingId : $("#settingProfile").val()},
				function(returnCode) {
						showMessage("success","Profile settings updated successfully",5000);
				}
			).error(function(xhr, textStatus, errorThrown){
				
				showMessage('error',"Invalid parameters",5000);
			});
	});
	
	$("#createProfile").click(function() {
		
		
		$("#dialog-createSettingsProfile").dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'create': function() {
					$(this).dialog("close");
						$.post(  
							starexecRoot+"secure/add/profile",
							{postp: $("#editPostProcess").val(), prep: $("#editPreProcess").val(), benchp: $("#editBenchProcess").val(),
								solver: $("#solver").val(), name: $("#settingName").val(), cpu: $("#cpuTimeout").val(),
								wall: $("#wallclockTimeout").val(), dep: $("#editDependenciesEnabled").val(),
								bench: $("#benchmark").val(), mem: $("#maxMem").val()},
							function(returnCode) {
									showMessage("success","Profile created successfully",5000);
							}
						).error(function(xhr, textStatus, errorThrown){
							showMessage('error',"Invalid parameters",5000);
						});
														
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});
	});
	
	//save the selected profile as a default
	$("#setDefaultProfile").click(function() {
		curSettingId=getSelectedSettingId();
		$.post(
				starexecRoot+"services/set/defaultSettings/"+ curSettingId,
				function(returnData){
					s=parseReturnCode(returnData);
				},
				"json"
		);
	});
	
	//delete the selected DefaultSettings profile
	$("#deleteProfile").click(function() {
		curSettingId=getSelectedSettingId();
		$.post(
				starexecRoot+"services/delete/defaultSettings/"+ curSettingId,
				function(returnData){
					s=parseReturnCode(returnData);
					if (s) {
						$(".settingOption[value='"+curSettingId+"']").remove();
					}

				},
				"json"
		);
	});
	$("#useSolver").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#useSolver").click(function(e) {
		useSelectedSolver();
		e.preventDefault();
	});
	
	$("#useBenchmark").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#useBenchmark").click(function(e) {
		useSelectedBenchmark();
		e.preventDefault();
	});
	
	 $("#solverList").dataTable({ 
			"sDom"			: 'rt<"bottom"flpi><"clear">',
			"iDisplayStart"	: 0,
			"iDisplayLength": defaultPageSize,
			"bServerSide"	: true,
			"sAjaxSource"	: starexecRoot+"services/",
			"sServerMethod" : 'POST',
			"fnServerData"	: fnSolverPaginationHandler
		});
	    $("#solverList").on("mousedown", "tr",function() {
			if ($(this).hasClass("row_selected")) {
				$(this).removeClass("row_selected");
			} else {
				$("#solverList").find("tr").removeClass("row_selected");
				$(this).addClass("row_selected");
			}
		});
	    
	    $("#benchmarkList").dataTable({ 
			"sDom"			: 'rt<"bottom"flpi><"clear">',
			"iDisplayStart"	: 0,
			"iDisplayLength": defaultPageSize,
			"bServerSide"	: true,
			"sAjaxSource"	: starexecRoot+"services/",
			"sServerMethod" : 'POST',
			"fnServerData"	: fnBenchmarkPaginationHandler
		});
	    $("#benchmarkList").on("mousedown", "tr",function() {
			if ($(this).hasClass("row_selected")) {
				$(this).removeClass("row_selected");
			} else {
				$("#benchmarkList").find("tr").removeClass("row_selected");
				$(this).addClass("row_selected");
			}
		});
	
	//this marks all community settings profiles with the text "(community)" so users can tell
	    $('#settingProfile > [type=COMMUNITY]').each(function() {
	    	
	    	$(this).text($(this).text() + " (community profile)");
	    });
}

function fnSolverPaginationHandler(sSource,aoData,fnCallback) {
	fnPaginationHandler(sSource,aoData,fnCallback,"solvers");
}
function fnBenchmarkPaginationHandler(sSource,aoData,fnCallback) {
	fnPaginationHandler(sSource,aoData,fnCallback,"benchmarks");
}



function fnPaginationHandler(sSource, aoData, fnCallback,prim){
	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "users/"+prim+"/pagination",
			aoData,
			function(nextDataTablePage){
				s=parseReturnCode(nextDataTablePage);
				if (s) {
					
				
					// Replace the current page with the newly received page
					fnCallback(nextDataTablePage);
				}
			},  
			"json"
	);
}


/**
 * Monitors the client's "websites" and updates the server if the client adds/deletes any
 */
function attachWebsiteMonitor(){
	// Handles deleting an existing website
	$("#websites").on( "click", ".delWebsite", function(){
		var id = $(this).attr('id');
		var parent = $(this).parent().parent();
		$('#dialog-confirm-delete-txt').text('Are you sure you want to delete this website?');
		
		$('#dialog-confirm-delete').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(
							starexecRoot+"services/websites/delete/"+ id,
							function(returnData){
								s=parseReturnCode(returnData);
								if (s) {
									parent.remove();
								}
	
							},
							"json"
					).error(function(){
						showMessage('error',"Internal error updating user websites",5000);
					});
				},
				"cancel": function() {
					$(this).dialog("close");
				}
			}
		});

	});
	
	// Handles adding a new website
	$("#addWebsite").click(function(){
		var name = $("#website_name").val();
		var url = $("#website_url").val();
		
		if(name.trim().length == 0) {
			showMessage('error', 'please enter a website name', 6000);
			return;
		} else if (url.indexOf("http://") != 0) {			
			showMessage('error', 'url must start with http://', 6000);
			return;
		} else if (url.trim().length <= 12) {
			showMessage('error', 'the given url is not long enough', 6000);
			return;
		}	
		
		var data = {name: name, url: url};
		$.post(
				starexecRoot+"services/website/add/user/-1",
				data,
				function(returnCode) {
					s=parseReturnCode(returnCode);
					if (s) {
						$("#website_name").val("");
			    		$("#website_url").val("");
			    		$('#websites li').remove();
			    		refreshUserWebsites();
					}
				},
				"json"
		);
	});
}

/**
 * Monitors the client's "change password" field and updates the server if the client uses it
 * to change their password
 */
function attachPasswordMonitor(){
	$('#changePass').click(function() {
		// Display the password strength meter
		$('#pwd-meter').show();
		var isFormValid = $("#changePassForm").valid();
		if(true == isFormValid){
			var currentPass = $('#current_pass').val();
			var newPass = $('#password').val();
			var confirmPass = $('#confirm_pass').val();
			
			var data = {current: currentPass, newpass: newPass, confirm: confirmPass};
			$.post(
					starexecRoot+"services/edit/user/password/"+userId,
					data,
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							$('#current_pass').val("");
							$('#password').val("");
							$('#confirm_pass').val("");
							$('#pwd-meter').hide();
						}
					},
					"json"
			);
		}
	});
}

/**
 * Initializes the JQuery icons for the buttons used on this page
 */
function initButtonIcons(){
	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
		}
	});
	
	$('#changePass').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	
	$('#uploadPicture').button({
		icons: {
			primary: "ui-icon-gear"
		}
    });	
}

/**
 * Queries the server for the user's websites and displays them on the page
 */
function refreshUserWebsites(){
	location.reload();

}

/**
 * Toggles the plus-minus text of the "+ add new" website button
 */
function togglePlusMinus(addSiteButton){
	if($(addSiteButton).children('span:first-child').text() == "+"){
		$(addSiteButton).children('span:first-child').text("-");
	} else {
		$(addSiteButton).children('span:first-child').text("+");
	}
}



/**
 * Allows for a given field to be editable
 */
function editable(attribute) {
	$('#edit' + attribute).click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();
		$('#save' + attribute).click(function(){saveChanges(this, true, attribute, old);});
		$('#cancel' + attribute).click(function(){saveChanges(this, false, attribute, old);});
	});	
	
	$('#save' + attribute).button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	
	$('#cancel' + attribute).button({
		icons: {
			secondary: "ui-icon-close"
		}
	});
}

/**
 * Updates the server with the new data a client has saved to 
 * either the 'firstname', 'lastname' or 'institution' fields
 * @param obj the DOM object that was edited 
 * @param save a boolean value as to whether or not we should apply these new values to the server
 * @param attr the name of the field that was edited (i.e. either 'firstname', 'lastname' or 'institution')
 * @param old the old values to apply if save = false
 */
function saveChanges(obj, save, attr, old) {
	
	var userId = window.location.search.split('id=')[1]; 

	if (true == save) {
		var newVal = $(obj).siblings('input:first').val();
		var unmodifiedNewVal = newVal;
		
		// Fixes 'session expired' bug that would occur if user inputed the empty String
		newVal = (newVal == "") ? "-1" : newVal;

		if (attr === 'diskquota') {
			// Convert input values like 1 KB to 1000
			newVal = convertToBytes(newVal);
		}
		
		$.post(  
				starexecRoot+"services/edit/user/" + attr + "/" + userId + "/" + newVal,
			    function(returnCode){  		
					s=parseReturnCode(returnCode);
					if (s) {
						// Change newVal to original in case above code modified newVal before call to post
						newVal = unmodifiedNewVal;

						// Hide the input box and replace it with the table cell
			    		$(obj).parent().after('<td id="edit' + attr + '">' + newVal + '</td>').remove();
			    		// Make the value editable again
			    		editable(attr);
					} else {
						$(obj).parent().after('<td id="edit' + attr + '">' + old + '</td>').remove();
			    		// Make the value editable again
			    		editable(attr);
					}
			     },  
			     "json"  
		).error(function(){
			showMessage('error',"Internal error updating user information",5000);
		});
	} else {
		// Hide the input box and replace it with the table cell
		$(obj).parent().after('<td id="edit' + attr + '">' + old + '</td>').remove();
		// Make the value editable again
		editable(attr);
	}
}

/**
 * Takes a String that represents a number of bytes in
 * GB, MB, KB, or Bytes. Returns a String that represents
 * the number of bytes with no units specified.
 * @author Albert Giegerich
 */
function convertToBytes(bytesOfAnyUnits) {
	var inputStrings = bytesOfAnyUnits.split(' ');
	
	if (inputStrings.length === 2) {
		var quotaValue = inputStrings[0];
		var byteUnits  = inputStrings[1].toUpperCase(); 	
		if (byteUnits === 'B' || byteUnits === 'BYTES') {
			return quotaValue;
		} else if (byteUnits === 'KB') {
			return (parseInt(quotaValue)*1024).toString();
		} else if (byteUnits === 'MB') {
			return (parseInt(quotaValue)*Math.pow(1024, 2)).toString();
		} else if (byteUnits === 'GB') {
			return (parseInt(quotaValue)*Math.pow(1024, 3)).toString();
		}
	} 
	// Return unaltered input if input was not formatted properly.
	return bytesOfAnyUnits;
}

	
/**
 * Displays the picture in the URI in a JQuery modal window
 */
function popUp(uri) {
	imageDialog = $("#popDialog");
	imageTag = $("#popImage");
	
	imageTag.attr('src', uri);
	imageTag.load(function(){
		$('#popDialog').dialog({
			dialogClass: "popup",
			modal: true,
			resizable: false,
			draggable: false,
			height: 'auto',
			width: 'auto'
		});
	});  
}
