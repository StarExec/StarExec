var defaultPPId = 0;

$(document).ready(function(){
	refreshSpaceWebsites();
	initUI();
	attachFormValidation();
	attachWebsiteMonitor();
});

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
					"/starexec/services/websites/delete/" + "space" + "/" + $('#comId').val() + "/" + id,
					function(returnData){
						if (returnData == 0) {
							parent.remove();
						} else {
							showMessage('error', "the website was not deleted due to an error; please try again", 5000);
						}
					},
					"json"
			).error(function(){
				alert('Session expired');
				window.location.reload(true);
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
				"/starexec/services/website/add/space/" + $('#comId').val(),
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
	
	$('fieldset:not(:first)').expandable(true);
	
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
	
	var formsToValidate = ['#addPreProcessorForm', '#addPostProcessorForm', '#newTypeForm', '#updatePstPrcssForm'];
	
	$('#updateBenchTypeForm').validate({
		rules : {
			name : {
				required : true,
				maxlength: $("#benchName").attr("length")
			},
			desc: {
				required : true,
				maxlength: $('#benchDesc').attr('length')
			},
			file: {
				required: true
			}
			
		},
		messages : {
			name : {
				required : 'please input a name',
				maxlength: $("#benchName").attr("length")+" characters maximum"
			},
			desc : {
				required : "please input a description",
				maxlength: $('#benchDesc').attr('length') + " characters maximum"
			},
			file: {
				required: "please choose a file"
			}
		}
	});
	
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
	$.getJSON('/starexec/services/websites/space/' + $("#comId").val(), processWebsiteData).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
}

/**
 * Processes website data by adding a delete button to the HTML and inject that into the DOM
 */
function processWebsiteData(jsonData) {
	// Injects the clickable delete button that's always present
	$('#websiteTable tr').remove();
	$.each(jsonData, function(i, site) {
		$('#websiteTable').append('<tr><td><a href="' + site.url + '">' + site.name + '<img class="extLink" src="/starexec/images/external.png"/></a></td><td><a class="delWebsite" id="' + site.id + '">delete</a></td></tr>');
	});
}


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
				showMessage('error', $('#descRow').attr('length')+ " characters maximum");
				return;
			}
		} else if (attr == 'name') {
			newVal = $(obj).siblings('input:first').val();
			if (newVal.length>$('#nameRow').attr('length')) {
				showMessage('error', $('#nameRow').attr('length')+ " characters maximum");
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
		if ($(this).is('.headerRow')) {
			
			return;
		}
		$(table).undelegate('tr');
		$(table).find('tr:not(:first)').css('cursor', 'default');		
		$(table).find('tr:not(:first)').addClass('noHover');
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
		$(deleteBtn).click(function(){ 
			deleteProcessor($("[name=pid]").val(), $(this).parent().parent(), table);
		});
		
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
	$(table).find('tr:not(:first)').removeClass('noHover');
	$(table).find('tr:first').find('th:last').remove();	
	$(table).find('tr.selected').html(oldHtml);
	$(table).find('tr.selected').removeClass('selected');	
	processorEditable(table);
}

function deleteProcessor(pid, parent, table){	
	$(table).find('tr:not(:first)').removeClass('noHover');
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
								$(table).find('tr:first').find('th:last').remove();	
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
	$(table).find('tr:not(:first)').removeClass('noHover');
	if (true == save) {
		var newVal = $(obj).siblings('input:first').val();
		if(newVal == null) {
			newVal = $(obj).siblings('textarea:first').val();			
		}		
		
		$.post(  
			    "/starexec/services/space/" + $('#comId').val() + "/processors/edit/" + attr,
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