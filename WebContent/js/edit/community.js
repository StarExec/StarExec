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
	
	var formsToValidate = ['#addPreProcessorForm', '#newTypeForm'];
	
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
		showMessage('error',"Internal error getting websites",5000);
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