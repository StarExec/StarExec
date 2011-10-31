$(document).ready(function(){
	$.getJSON('/starexec/services/websites', displayWebsites).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	editable();
	$('#personal tr:even').addClass('shade');
	$('#password tr:even').addClass('shade');
});

function displayWebsites(data) {
	$.each(data, function(i, site) {
		var shade = !(i % 2) ? ' class="shade"' : '';
		$('#websites').append('<li' + shade + '><a href="' + site.url + '">' + site.name + '</li>');
	});
}

function editable() {
	$('#editfirstname').click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveFirst">save</button>&nbsp;<button id="cancelFirst">cancel</button>&nbsp;</td>').remove();
		$('#saveFirst').click(function(){saveChanges(this, true, "firstname");});
		$('#cancelFirst').click(function(){saveChanges(this, old, "firstname");});
	});
	$('#editlastname').click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveLast">save</button>&nbsp;<button id="cancelLast">cancel</button>&nbsp;</td>').remove();
		$('#saveLast').click(function(){saveChanges(this, true, "lastname");});
		$('#cancelLast').click(function(){saveChanges(this, old, "lastname");});
	});
	$('#editinstitution').click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveInst">save</button>&nbsp;<button id="cancelInst">cancel</button>&nbsp;</td>').remove();
		$('#saveInst').click(function(){saveChanges(this, true, "institution");});
		$('#cancelInst').click(function(){saveChanges(this, old, "institution");});
	});
	$('#editemail').click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveMail">save</button>&nbsp;<button id="cancelMail">cancel</button>&nbsp;</td>').remove();
		$('#saveMail').click(function(){saveChanges(this, true, "email");});
		$('#cancelMail').click(function(){saveChanges(this, old, "email");});
	});
	$('#changePass').click(function() {
		var currentPass = document.getElementById('current_pass').value;
		var newPass = document.getElementById('new_pass').value;
		var confirmPass = document.getElementById('confirm_pass').value;
		
		savePass(currentPass, newPass, confirmPass);
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
	
	//Make the page editable again
	editable();
}

function savePass(curr, new1, new2) {
	var data = {current: curr, newpass: new1, confirm: new2};
	$.post(
			"/starexec/services/edit/user/password/",
			data,
			function(returnCode) {
				switch (returnCode) {
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
	
	editable();
}