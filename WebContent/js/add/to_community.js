$(document).ready(function() {
	initUI();
	attachFormValidation();
});


/**
 * Sets up user-interface related things
 */
function initUI(){
	
	// Set the default text to display for the textarea
	monitorTextarea("#reason", "describe your motivation for joining this community");
	
	
	// Don't permit the default text as a message to the leaders
	$("#btnSubmit").click(function(){
		if ($("#reason").val() == $("#reason").data('default')){
			$("#reason").val("");
	    }
	});
	
	$('#btnSubmit').button({
		icons: {
			secondary: "ui-icon-mail-closed"
		}
	});
}


/**
 * Sets a default message for a textarea that is cleared when the textarea
 * receives focus and only returns if the textarea is empty when it loses focus
 */
function monitorTextarea(textarea, defaultText){
	// Set the default text
	$(textarea).data("default", defaultText);
	$(textarea).val($(textarea).data("default"));

	// Clear the textarea when clicked on if the text in there == the default text
	// and re-insert that default text if the user doesn't input anything
	$(textarea)
	  .focus(function() {
	        if (this.value === $(textarea).data("default")) {
	            this.value = "";
	        }
	  })
	  .blur(function() {
	        if (this.value === "") {
	            this.value = $(textarea).data("default");
	        }
	});
}


/**
 * Attaches form validation to the community membership request form
 */
function attachFormValidation(){
	
	// Add regular expression method to JQuery validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	
	// Form validation
	$("#inviteForm").validate({
		rules : {
			cm: {
				required: true
			},
			msg: {
				required : true,
				minlength: 2,
				maxlength: 512,
				regex	 : getPrimDescRegex()
			}
		},
		messages : {
			msg: {
				required : "enter your reason for joining",
				minlength: "2 characters minimum",
				maxlength: "512 charactes maximum",
				regex	 : "invalid character(s)"
			}
		},
		// Place the error messages in the tooltip instead of in the DOM
		errorPlacement: function (error, element) {
			if($(error).text().length > 0){
				//$(element).qtip('api').updateContent('<b>'+$(error).text()+'</b>', true);
			}
		},
		// Hide the error tooltip when no errors are present
		success: function(label){
			//$('#' + $(label).attr('for')).qtip('api').hide();
		}
	});
}

