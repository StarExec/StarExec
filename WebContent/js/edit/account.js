$(document).ready(function(){
	initUI();
	attachFormValidation();
	attachPasswordMonitor();
	attachArchiveTypeMonitor();
	attachWebsiteMonitor();
	refreshUserWebsites();
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
	
	initDataTables();
	initButtonIcons();
	
	// Collapse all fieldsets on page load except for the one containing the client's information
	$('fieldset:first').expandable(false);
	$('fieldset:not(:first)').expandable(true);
	
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
	
	// If the client's picture is clicked on, pop it up in a JQuery modal window
	$('#showPicture').click(function(event){
		popUp($(this).attr('enlarge'));
	});
	
	// Close the modal frame if the client clicks outside of it
	$(document).click(function(e) {
		if (!$(e.target).parents().filter('.ui-dialog').length) {
			$('#popDialog').dialog('close');
		}
	});
}



/**
 * Monitors the client's "preferred archive type" and updates the server if the
 * client changes it
 */
function attachArchiveTypeMonitor(){
	$("#selectArchive").change(function() {
		var value = $("#selectArchive").val();
		$.post(  
				starexecRoot+"services/edit/user/archivetype/" + value,
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		showMessage('success', "preferred archive type successfully updated", 5000);
			    	} else {
			    		showMessage('error', "preferred archive type not changed; please try again", 5000);
			    	}
			     },  
			     "json"  
		).error(function(){
			showMessage('error',"Internal error updating preferred archive type",5000);
		});
	});
}

/**
 * Monitors the client's "websites" and updates the server if the client adds/deletes any
 */
function attachWebsiteMonitor(){
	// Handles deleting an existing website
	$("#websites").delegate(".delWebsite", "click", function(){
		var id = $(this).attr('id');
		var parent = $(this).parent().parent();
		var answer = confirm("are you sure you want to delete this website?");
		if (true == answer) {
			$.post(
					starexecRoot+"services/websites/delete/" + "user" + "/" + -1 + "/" + id,
					function(returnData){
						if (returnData == 0) {
							parent.remove();
						} else {
							showMessage('error', "the website was not deleted due to an error; please try again", 5000);
						}
					},
					"json"
			).error(function(){
				showMessage('error',"Internal error updating user websites",5000);
			});
		}
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
			    	if(returnCode == '0') {
			    		$("#website_name").val("");
			    		$("#website_url").val("");
			    		$('#websites li').remove();
			    		refreshUserWebsites();
			    	} else {
			    		showMessage('error', "error: website not added. please try again", 5000);
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
					starexecRoot+"services/edit/user/password/",
					data,
					function(returnCode) {
						switch (returnCode) {
							case 0:		// Successfully changed password
								showMessage('success', "password successfully changed", 5000);
								$('#current_pass').val("");
								$('#password').val("");
								$('#confirm_pass').val("");
								$('#pwd-meter').hide();
								break;
							case 2:		// Parameter validation failed
								showMessage('error', "password must be between 6-20 characters, contains at least one character, one number, and one punctuation mark", 10000);
								break;
							case 3:		// 'new password' & 'confirm password' fields did not match
								showMessage('error', "make sure to confirm the new password; please try again", 5000);
								break;
							case 4:		// Incorrect 'current password'
								showMessage('error', "incorrect current password; please try again", 5000);
								$('#current_pass').val("");
								$("#changePassForm").valid();
								break;
							default:	// Database error
								showMessage('error', "password update not successful; please try again", 5000);
								break;
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
	$.getJSON(starexecRoot+'services/websites/user/-1', processWebsiteData).error(function(){
		showMessage('error',"Internal error displaying user websites",5000);
	});
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
 * Extracts, formats, and injects the websites returned from the server into the client's DOM 
 */
function processWebsiteData(jsonData) {
	// Ensures the websites table is empty
	$('#websites tbody tr').remove();
	
	// Build the HTML to display the website and a delete button, then inject that into the client's DOM
	$.each(jsonData, function(i, site) {
		$('#websites tbody').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src=starexecRoot+"images/external.png"/></a></td><td><a class="delWebsite" id="' + site.id + '">delete</a></td></tr>');
	});
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
	if (true == save) {
		var newVal = $(obj).siblings('input:first').val();
		
		// Fixes 'session expired' bug that would occur if user inputed the empty String
		newVal = (newVal == "") ? "-1" : newVal;
		
		$.post(  
				starexecRoot+"services/edit/user/" + attr + "/" + newVal,
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