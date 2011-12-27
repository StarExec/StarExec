$(document).ready(function(){
	//get website information for the current user
	$.getJSON('/starexec/services/websites/user/-1', displayWebsites).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	
	$('#toggleWebsite').click(function() {
		$('#new_website').slideToggle('fast');	
	});	
	$('#new_website').hide();
	
	//how to add a new website
	$("#addWebsite").click(function(){
		var name = $("#website_name").val();
		var url = $("#website_url").val();
		
		var data = {name: name, url: url};
		$.post(
				"/starexec/services/website/add/user/-1",
				data,
				function(returnCode) {
			    	if(returnCode == '0') {
//			    		showMessage('success', "website successfully added", 5000);
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
		$("#website_name").val("");
		$("#website_url").val("");
	});
	
	//make the various parts editable
	editable("firstname");
	editable("lastname");
	editable("institution");
	
	//make the password updatable
	$('#changePass').click(function() {
		var currentPass = document.getElementById('current_pass').value;
		var newPass = document.getElementById('new_pass').value;
		var confirmPass = document.getElementById('confirm_pass').value;
		
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
							showMessage('success', "password update successful", 5000);
							break;
						case 1:
							showMessage('error', "password update not successful; please try again", 5000);
							break;
						case 2:
							showMessage('error', "illegal password; please try again", 5000);
							break;
						case 3:
							showMessage('error', "make sure to confirm the new password; please try again", 5000);
							break;
						case 4:
							showMessage('error', "incorrect current password; please try again", 5000);
							break;
						default:
							showMessage('error', "update failed", 5000);
							break;
					}
				},
				"json"
		);
	});
	
	//styling
	$('#personal tr:even').addClass('shade');
	$('#password tr:even').addClass('shade');
});

function displayWebsites(data) {
	// Injects the clickable delete button that's always present
	$.each(data, function(i, site) {
		$('#websites tr').parent().remove();
		$('#websites').append('<li><a href="' + site.url + '">' + site.name + '</a><a class="website" id="' + site.id + '">delete</a></li>');
		$('#websites li:even').addClass('shade');
	});
	
	// Handles deletion of websites
	$('.website').click(function(){
		var id = $(this).attr('id');
		var parent = $(this).parent();
		var answer = confirm("are you sure you want to delete this website?");
		if (true == answer) {
			$.post(
					"/starexec/services/websites/delete/" + "user" + "/" + -1 + "/" + id,
					function(returnData){
						if (returnData == 0) {
//							showMessage('success', "website sucessfully deleted", 5000);
							parent.remove();
				    		$('#websites li').removeClass('shade');
				    		$('#websites li:even').addClass('shade');
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
		$('#save' + attribute).click(function(){saveChanges(this, true, attribute);});
		$('#cancel' + attribute).click(function(){saveChanges(this, old, attribute);});
	});
}

function saveChanges(obj, save, attr) {
	var t = save;
	if (true == save) {
		t = $(obj).siblings('input:first').val();
		$.post(  
			    "/starexec/services/edit/user/" + attr + "/" + t,  
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		showMessage('success', "information successfully updated", 5000);
			    	} else {
			    		showMessage('error', "error: information not changed. please try again", 5000);
			    	}
			     },  
			     "json"  
		).error(function(){
			alert('Session expired');
			window.location.reload(true);
		});
	}
	
	//Hide the input box and replace it with the table cell
	$(obj).parent().after('<td id="edit' + attr + '">' + t + '</td>').remove();
	
	//Make the value editable again
	editable(attr);
}
