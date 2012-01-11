$(document).ready(function(){
	$.getJSON('/starexec/services/websites/space/' + $("#comId").val(), displayWebsites).error(function(){
		alert('Session expired');
		window.location.reload(true);
	});
	
	// Make forms editable
	editable("name");
	editable("desc");	
	typeEditable();
	
	// Style the table with alternate rows
	$('#details tr:even').addClass('shade');
	$('#websites li:even').addClass('shade');
	$('#benchTypes tr:even').addClass('shade');	
	
	// Add toggles for the "add new" buttons and hide them by default
	$('#toggleWebsite').click(function() {
		$('#new_website').slideToggle('fast');
		togglePlusMinus(this);
	});	
	$('#toggleType').click(function() {
		$('#newType').slideToggle('fast');
		togglePlusMinus(this);
	});	
	$('#new_website').hide();
	$('#newType').hide();
	
	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Form validation
	$("#typeForm").validate({
		rules : {
			typeName : {
				required : true,
				regex : "^[a-zA-Z0-9\\-\\s_]+$",
				minlength : 2,
				maxlength: 32
			},
			typeDesc : {
				required : false,	
				regex : "^[a-zA-Z0-9\\-\\s_.!?/,\\\\+=\"'#$%&*()\\[{}\\]]+$",
				maxlength: 300
			},
			typeFile : {
				required : true				
			}			
		},
		messages : {
			typeName : {
				required : "enter a type name",
				minlength : "needs to be at least 2 characters",
				maxlength : "32 characters max",
				regex : "invalid characters"
			},
			typeDesc : {				
				maxlength : "no more than 300 characters",	
				regex : "invalid characters"
			},
			typeFile : {
				required : "choose a file"				
			}
		}		
	});
	
	$('#addType').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});	
	
	$('#addWebsite').button({
		icons: {
			secondary: "ui-icon-plus"
    }});		
});

function displayWebsites(data) {
	// Injects the clickable delete button that's always present
	$.each(data, function(i, site) {
		$('#websites tr').parent().remove();
		$('#websites').append('<li><a href="' + site.url + '">' + site.name + '<img class="extLink" src="/starexec/images/external.png"/></a><a class="delWebsite" id="' + site.id + '">delete</a></li>');
		$('#websites li:even').addClass('shade');
	});
	
	// Handles deletion of websites
	$('.delWebsite').click(function(){
		var answer = confirm("are you sure you want to delete this website?");
		var websiteId = $(this).attr('id');
		var parent = $(this).parent();
		if (true == answer) {
			$.post(
					"/starexec/services/websites/delete/" + "space" + "/" + $('#comId').val() + "/" + websiteId,
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
			    		$('#websites li').remove();
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

var x= 0;

function typeEditable() {			
	$('#benchTypes').delegate('tr', 'click', function(){
		$('#benchTypes').undelegate('tr');
		$('#benchTypes tr').css('cursor', 'default');		
		
		$('#benchTypes').find('tr:first').append('<th>action</th>');
		var old = $(this).html();
		$(this).addClass('selected');
		
		var tName = $(this).children(':first');
		var tDesc = $(this).children(':nth-child(2)');
		var tFile = $(this).children(':nth-child(3)');
		
		$(tName).html('<input type="text" name="typeName" value="' +  $(tName).text() + '" />');
		$(tDesc).html('<textarea name="typeDesc">' + $(tDesc).text() + '</textarea>');
		$(tFile).html('<input type="file" name="typeFile" />');
		$(this).append('<input type="hidden" name="typeId" value="' + $(this).attr('id').split('_')[1] + '" />');
				
		var saveBtn = $('<button type="submit">save</button><br/>');
		var cancelBtn = $('<button type="button">cancel</button><br/>');
		var deleteBtn = $('<button type="button">delete</button>');
		var saveCol = $('<td></td>').append(saveBtn).append(cancelBtn).append(deleteBtn);			
		$(this).append(saveCol);
		
		$(saveBtn).click(function(){saveChanges(this, true, attribute);});
		$(cancelBtn).click(function(){ restoreBenchTypeRow(old); });			
		$(deleteBtn).click(function(){ deleteType($("[name=typeId]").val(), $(this).parent().parent()); });
		
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
	
	$('#benchTypes tr').css('cursor', 'pointer');
}

function restoreBenchTypeRow(old) {	
	$('#benchTypes').find('tr:first').find('th:last').remove();	
	$('#benchTypes tr.selected').html(old);
	$('#benchTypes tr.selected').removeClass('selected');	
	typeEditable();
}

function deleteType(typeId, parent){
	if(confirm("are you sure you want to delete this benchmark type?")){
		$.post(
				"/starexec/services/edit/space/type/delete/" + $('#comId').val() + "/" + typeId,
				function(returnCode) {
					if(returnCode == '0') {
//		    		showMessage('success', "type was successfully deleted", 5000);
						parent.remove();
						$('#benchTypes tr').removeClass('shade');
						$('#benchTypes tr:even').addClass('shade');	
					} else {
						showMessage('error', "error: type not added. please try again", 5000);
					}
				},
				"json"
		).error(function(){
			alert('Session expired');
			window.location.reload(true);
		});
	}
}


function saveChanges(obj, save, attr, old) {
	if (true == save) {
		var newVal = $(obj).siblings('input:first').val();
		if(newVal == null) {
			newVal = $(obj).siblings('textarea:first').val();			
		}		
		
		$.post(  
			    "/starexec/services/edit/space/" + attr + "/" + $('#comId').val(),
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