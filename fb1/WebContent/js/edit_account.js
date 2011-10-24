$(document).ready(function(){
	$.getJSON('/starexec/services/websites', displayWebsites);
	editable();
});

function displayWebsites(data) {
	jsonData = $.parseJSON(data);
	var i = 0;
	$.each(jsonData.websites, function() {
		if (i % 2) {
			$('#websites').append('<tr><td>' + this.name + '</td><td>' + this.url + '</td></tr>');
		} else {
			$('#websites').append('<tr class="shade"><td>' + this.name + '</td><td>' + this.url + '</td></tr>');
		}
		i++;
	});
}

function editable() {
	$('#editfirstname').click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveFirst">save</button>&nbsp;<button id="cancelFirst">cancel</button>&nbsp;</td>').remove();
		$('#saveFirst').click(function(){saveChanges(this, true, "firstname");});
		$('#cancelFirst').click(function(){saveChanges(this, old, "firstname");});
	});
	$("#editlastname").click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveLast">save</button>&nbsp;<button id="cancelLast">cancel</button>&nbsp;</td>').remove();
		$('#saveLast').click(function(){saveChanges(this, true, "lastname");});
		$('#cancelLast').click(function(){saveChanges(this, old, "lastname");});
	});
	$("#editinstitution").click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveInst">save</button>&nbsp;<button id="cancelInst">cancel</button>&nbsp;</td>').remove();
		$('#saveInst').click(function(){saveChanges(this, true, "institution");});
		$('#cancelInst').click(function(){saveChanges(this, old, "institution");});
	});
	$("#editemail").click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" value="' + old + '" />&nbsp;<button id="saveMail">save</button>&nbsp;<button id="cancelMail">cancel</button>&nbsp;</td>').remove();
		$('#saveMail').click(function(){saveChanges(this, true, "email");});
		$('#cancelMail').click(function(){saveChanges(this, old, "email");});
	});
	$("#changePass").click(function() {
		//TODO store values from input, call a savepass function to make the POST call.
		//hash it on the client side?
	});
}

function saveChanges(obj, save, attr) {
	if (true == save) {
		var t = $(obj).siblings('input:first').val();
		$.post(  
			    "/starexec/services/edit/user/" + attr + "/" + t,  
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		showMessage('success', "information successfully updated");
			    	} else {
			    		showMessage('error', "error: information not changed. please try again");
			    	}
			     },  
			     "json"  
		);
	}
	else {
		var t = save;
	}
	$(obj).parent().after('<td id="edit' + attr + '">' + t + '</td>').remove();

	//Make the page editable again.
	editable();
}