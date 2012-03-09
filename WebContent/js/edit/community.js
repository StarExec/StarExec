$(document).ready(function(){
	$.getJSON('/starexec/services/websites/space/' + $("#comId").val(), displayWebsites).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	
	// Make forms editable
	editable("name");
	editable("desc");
	processorEditable($('#benchTypeTbl'));
	processorEditable($('#preProcessorTbl'));
	processorEditable($('#postProcessorTbl'));
	
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
	
	
	$('#newWebsite').hide();
	$('#newTypeTbl').hide();
	$('#newPostProcessTbl').hide();
	$('#newPreProcessTbl').hide();
	$('#dialog-confirm-delete').hide();
	
	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	var formsToValidate = ['#addPreProcessorForm', '#addPostProcessorForm', '#newTypeForm'];
	
	$.each(formsToValidate, function(i, selector) {
		$(selector).validate({
			rules : {
				name : {
					required : true,
					regex : "^[a-zA-Z0-9\\-\\s_]+$",
					minlength : 2,
					maxlength: 32
				},
				desc: {
					required : false,	
					regex : "^[a-zA-Z0-9\\-\\s_.!?/,\\\\+=\"'#$%&*()\\[{}\\]]+$",
					maxlength: 300
				},
				file: {
					required : true				
				}			
			},
			messages : {
				name : {
					required : "enter a processor name",
					minlength : "needs to be at least 2 characters",
					maxlength : "32 characters max",
					regex : "invalid characters"
				},
				desc : {				
					maxlength : "no more than 300 characters",	
					regex : "invalid characters"
				},
				file : {
					required : "choose a file"				
				}
			}		
		});	
	});	
	
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
	
	$('fieldset:not(:first)').expandable(true);
});

function displayWebsites(data) {
	// Injects the clickable delete button that's always present
	$('#websiteTable tr').remove();
	$.each(data, function(i, site) {
		$('#websiteTable').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src="/starexec/images/external.png"/></a></td><td><a class="delWebsite" id="' + site.id + '">delete</a></td></tr>');
	});
	
	// Handles deletion of websites
	$('.delWebsite').click(function(){
		var answer = confirm("are you sure you want to delete this website?");
		var websiteId = $(this).attr('id');
		var parent = $(this).parent().parent();
		if (true == answer) {
			$.post(
					"/starexec/services/websites/delete/" + "space" + "/" + $('#comId').val() + "/" + websiteId,
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
	
	// Add a new website functionality
	$("#addWebsite").click(function(){
		var name = $('#website_name').val();
		var url = $('#website_url').val();
		
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
				"/starexec/services/website/add/space/" + $('#comId').val(),
				data,
				function(returnCode) {
			    	if(returnCode == '0') {
			    		$('#website_name').val("");
			    		$('#website_url').val("");
			    		$('#websiteTable tr').remove();
			    		$.getJSON('/starexec/services/websites/space/' + $("#comId").val(), displayWebsites).error(function(){
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
	
}

function editable(attribute) {
	$('#edit' + attribute).click(function(){
		var old = $(this).html();
		
		if(attribute == "desc") {
			$(this).after('<td><textarea>' + old + '</textarea>&nbsp;<button id="save' + attribute + '">save</button>&nbsp;<button id="cancel' + attribute + '">cancel</button>&nbsp;</td>').remove();
		} else {
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
		if (attr = 'desc') {
			newVal = $(obj).siblings('textarea:first').val();
		} else {
			newVal = $(obj).siblings('input:first').val();
		}
		
		// Fixes 'session expired' bug that would occur if user inputed the empty String
		newVal = (newVal == "") ? "-1" : newVal;
		
		$.post(  
				"/starexec/services/edit/space/" + attr + "/" + getParameterByName("cid"),
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

function processorEditable(table) {			
	$(table).delegate('tr', 'click', function(){
		$(table).undelegate('tr');
		$(table).find('tr:not(:first)').css('cursor', 'default');		
		
		$(table).find('tr:first').append('<th>action</th>');
		var old = $(this).html();
		$(this).addClass('selected');
		
		var name = $(this).children(':first');
		var desc = $(this).children(':nth-child(2)');
		var file = $(this).children(':nth-child(3)');
		
		$(name).html('<input type="text" name="name" value="' +  $(name).text() + '" />');
		$(desc).html('<textarea name="desc">' + $(desc).text() + '</textarea>');
		$(file).html('<input type="file" name="file" />');
		$(this).append('<input type="hidden" name="pid" value="' + $(this).attr('id').split('_')[1] + '" />');
				
		var saveBtn = $('<button type="submit">save</button><br/>');
		var cancelBtn = $('<button type="button">cancel</button><br/>');
		var deleteBtn = $('<button type="button">delete</button>');
		var saveCol = $('<td></td>').append(saveBtn).append(cancelBtn).append(deleteBtn);			
		$(this).append(saveCol);
		
		$(saveBtn).click(function(){updateProcessor(this, true, attribute);});
		$(cancelBtn).click(function(){ restoreProcessorRow(table, old); });			
		$(deleteBtn).click(function(){ deleteProcessor($("[name=pid]").val(), $(this).parent().parent()); });
		
		$(saveBtn).button({
			icons: {
				secondary: "ui-icon-check"
	    }});
		
		$(cancelBtn).button({
			icons: {
				secondary: "ui-icon-close"
	    }});
		
		$(deleteBtn).button({
			icons: {
				secondary: "ui-icon-minus"
	    }});
	});
	
	$(table).find('tr:not(:first)').css('cursor', 'pointer');
}

function restoreProcessorRow(table, oldHtml) {	
	$(table).find('tr:first').find('th:last').remove();	
	$(table).find('tr.selected').html(oldHtml);
	$(table).find('tr.selected').removeClass('selected');	
	processorEditable(table);
}

function deleteProcessor(pid, parent){		
	$('#dialog-confirm-delete-txt').text('are you sure you want to delete this processor?');
	
	// Display the confirmation dialog
	$('#dialog-confirm-delete').dialog({
		modal: true,
		buttons: {
			'yes': function() {					
				// If the user actually confirms, close the dialog right away
				$('#dialog-confirm-delete').dialog('close');
				
				$.post(
						"/starexec/services/processors/delete/" + pid,
						function(returnCode) {
							if(returnCode == '0') {
								var table = $(parent).parents('table');
								parent.siblings('tr:first-child').children('th:last').remove();
								parent.remove();
								processorEditable(table);
							} else {
								showMessage('error', "processor could not be removed. please try again", 5000);
							}
						},
						"json"
				).error(function(){
					alert('Session expired');
					window.location.reload(true);
				});
			},
			"cancel": function() {
				$(this).dialog("close");
			}
		}		
	});	
}

function updateProcessor(obj, save, attr, old) {
	if (true == save) {
		var newVal = $(obj).siblings('input:first').val();
		if(newVal == null) {
			newVal = $(obj).siblings('textarea:first').val();			
		}		
		
		$.post(  
			    "/starexec/services/space/" + $('#comId').val() + "/procesors/edit/" + attr,
			    {val: newVal},
			    function(returnCode){  			        
			    	if(returnCode == '0') {
			    		// Hide the input box and replace it with the table cell
			    		$(obj).parent().after('<td id="edit' + attr + '">' + newVal + '</td>').remove();
			    		// Make the value editable again
			    		editable(attr);
			    	} else if(returnCode == '2') {
			    		showMessage('error', "insufficient permissions. only space leaders can update their space", 5000);
			    		// Hide the input box and replace it with the table cell
			    		$(obj).parent().after('<td id="edit' + attr + '">' + old + '</td>').remove();
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