$(document).ready(function(){
	//get website information for the current user
	$.getJSON('/starexec/services/websites/user/-1', displayWebsites).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	
	// Setup the 'add website' and 'change password' buttons
	initButtons();
	
	// Setup '+ add new' animation
	$('#toggleWebsite').click(function() {
		$('#new_website').slideToggle('fast');
		togglePlusMinus(this);
	});	
	$('#new_website').hide();

	// Hide the password strength meter initially and display it when a password's
	// strength has been calculated
	$.validator.passwordStrengthMeter("#pwd-meter");
	
	//make the various parts editable
	editable("firstname");
	editable("lastname");
	editable("institution");
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#changePassForm").submit(function(e){
		e.preventDefault();
	});
	
	// Validates change-password fields
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
		// the errorPlacement ignores #password & #reason
		errorPlacement : function(error, element) {
			if($(element).attr("id") != "password"){
				error.insertAfter(element);
			}
		}
	});
	
	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
    }});
	
	$('#changePass').button({
		icons: {
			secondary: "ui-icon-check"
    }});
	
	//handle changing of preferred archive type
	$("#selectArchive").change(function() {
		var value = $("#selectArchive").val();
		$.post(  
				"/starexec/services/edit/user/archivetype/" + value,
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		showMessage('success', "preferred archive type successfully updated", 5000);
			    	} else {
			    		showMessage('error', "preferred archive type not changed; please try again", 5000);
			    	}
			     },  
			     "json"  
		).error(function(){
			alert('Session expired');
			window.location.reload(true);
		});
	});
	
	$('fieldset:first').expandable(false);
	$('fieldset:not(:first)').expandable(true);
	
	$('#personal').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',
        "aaSorting": [],
        "bPaginate": false,        
        "bSort": true        
    });	
	
	$('img').click(function(event){
		PopUp($(this).attr('enlarge'));
	});
});


/**
 * Initializes the 'add website' and 'change password' buttons
 */
function initButtons(){
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
				"/starexec/services/website/add/user/-1",
				data,
				function(returnCode) {
			    	if(returnCode == '0') {
			    		$("#website_name").val("");
			    		$("#website_url").val("");
			    		$('#websites li').remove();
			    		$.getJSON('/starexec/services/websites/user/-1', displayWebsites).error(function(){
			    			alert('Session expired');
			    			window.location.reload(true);
			    		});
			    	} else {
			    		showMessage('error', "error: website not added. please try again", 5000);
			    	}
				},
				"json"
		);
		
	});
	
	
	// Handles changing user's password
	$('#changePass').click(function() {
		$('#pwd-meter').show();
		var isFormValid = $("#changePassForm").valid();
		if(true == isFormValid){
			var currentPass = $('#current_pass').val();
			var newPass = $('#password').val();
			var confirmPass = $('#confirm_pass').val();
			
			var data = {current: currentPass, newpass: newPass, confirm: confirmPass};
			$.post(
					"/starexec/services/edit/user/password/",
					data,
					function(returnCode) {
						switch (returnCode) {
						/*success/error message based on what gets returned
						0: successful
						1: database error
						2: did not pass validation
						3: new password and confirm password fields were different
						4: wrong current password for the user
						 */
						case 0:
							showMessage('success', "password successfully changed", 5000);
							$('#current_pass').val("");
							$('#password').val("");
							$('#confirm_pass').val("");
							$('#pwd-meter').hide();
							break;
						case 2:
							showMessage('error', "illegal password; please try again", 5000);
							break;
						case 3:
							showMessage('error', "make sure to confirm the new password; please try again", 5000);
							break;
						case 4:
							showMessage('error', "incorrect current password; please try again", 5000);
							$('#current_pass').val("");
							$("#changePassForm").valid();
							break;
						default:
							showMessage('error', "password update not successful; please try again", 5000);
						break;
						}
					},
					"json"
			);
		}
	});
	
	$('#uploadPicture').button({
		icons: {
			primary: "ui-icon-gear"
		}
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



function displayWebsites(data) {
	
	// Ensures the websites table is empty
	$('#websites tbody tr').remove();
	
	// Injects the clickable delete button that's always present
	$.each(data, function(i, site) {
		$('#websites tbody').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src="/starexec/images/external.png"/></a></td><td><a class="website" id="' + site.id + '">delete</a></td></tr>');
	});
	
	// Handles deletion of websites
	$('.website').click(function(){
		var id = $(this).attr('id');
		var parent = $(this).parent().parent();
		var answer = confirm("are you sure you want to delete this website?");
		if (true == answer) {
			$.post(
					"/starexec/services/websites/delete/" + "user" + "/" + -1 + "/" + id,
					function(returnData){
						if (returnData == 0) {
							parent.remove();
						} else {
							showMessage('error', "error: website not deleted. please try again", 5000);
						}
					},
					"json"
			).error(function(){
				alert('Session expired');
				window.location.reload(true);
			});
		}
	});
}

function editable(attribute) {
	$('#edit' + attribute).click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();
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
}


function saveChanges(obj, save, attr, old) {
	if (true == save) {
		var newVal = $(obj).siblings('input:first').val();
		
		// Fixes 'session expired' bug that would occur if user inputed the empty String
		newVal = (newVal == "") ? "-1" : newVal;
		
		$.post(  
				"/starexec/services/edit/user/" + attr + "/" + newVal,
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
			alert('Session expired');
			window.location.reload(true);
		});
	} else {
		// Hide the input box and replace it with the table cell
		$(obj).parent().after('<td id="edit' + attr + '">' + old + '</td>').remove();
		// Make the value editable again
		editable(attr);
	}
}
	
function PopUp(uri) {
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
			width: 'auto',
		});
	});  
}