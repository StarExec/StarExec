$(document).ready(function(){
	//get website information for the given solver
	$.getJSON('/starexec/services/websites/solver/' + getParameterByName("id"), displayWebsites).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	
	
	// Attach click listeners to all the buttons
	initButtons();
	
	// Setup '+ add new' animation
	$('#toggleWebsite').click(function() {
		$('#new_website').slideToggle('fast');
		togglePlusMinus(this);
	});	
	$('#new_website').hide();
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editSolverForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Validates solver fields
	$("#editSolverForm").validate({
		rules : {
			name : {
				required : true,
				regex : "^[\\w\\-\\.\\s]+$"
			},
			description : {
				required : true
			}
		},
		messages : {
			name : {
				required : "name required",
				regex : "invalid characters"
			},
			description : {
				required : "description required"
			}
		}
	});
	
	$('#delete').button({
		icons: {
			secondary: "ui-icon-minus"
    }});
	
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
    }});
	
	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
    }});
	
	$('fieldset:first').expandable(false);
	$('fieldset:not(:first)').expandable(true);
});

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
				"/starexec/services/website/add/solver/" + getParameterByName("id"),
				data,
				function(returnCode) {
			    	if(returnCode == '0') {
			    		$("#website_name").val("");
			    		$("#website_url").val("");
			    		$('#websites li').remove();
			    		$.getJSON('/starexec/services/websites/solver/' + getParameterByName("id"), displayWebsites).error(function(){
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
	
	// Prompts user to confirm deletion and, if they confirm,
	// deletes the solver via AJAX, then redirects to explore/spaces.jsp
	$("#delete").click(function(){
		var confirm = window.confirm("are you sure you want to delete this solver?");
		if(confirm == true){
			$.post(
					"/starexec/services/delete/solver/" + getParameterByName("id"),
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/explore/spaces.jsp';
								break;
							case 1:
								showMessage('error', "solver was not deleted; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this solver can modify its details", 5000);
								break;
							default:
								showMessage('error', "invalid parameters", 5000);
								break;
						}
					},
					"json"
			);
		}
	});

	// Triggers validation and, if that passes,
	// updates the solver details via AJAX, then redirects to edit/solver.jsp
	$("#update").click(function(){
		var isFormValid = $("#editSolverForm").valid();
		if(isFormValid == true){
			var name = $("#name").val();
			var description = $("#description").val();
			var isDownloadable = $("#downloadable").is(':checked');
			var data = {name: name, description: description, downloadable: isDownloadable};
			$.post(
					"/starexec/services/edit/solver/" + getParameterByName("id"),
					data,
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/details/solver.jsp?id=' + getParameterByName("id");
								break;
							case 1:
								showMessage('error', "solver details were not updated; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this solver can modify its details", 5000);
								break;
							default:
								showMessage('error', "invalid parameters", 5000);
								break;
						}
					},
					"json"
			);
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
					"/starexec/services/websites/delete/solver/" + getParameterByName("id") + "/" + id,
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