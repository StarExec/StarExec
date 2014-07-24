$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Initializes user-interface 
 */
function initUI(){
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
}

/**
 * Attaches form validation to the picture upload field
 */
function attachFormValidation(){
	
	// Add regular expressions to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});

	// Re-validate the 'picture location' field when it loses focus
	$("#uploadPic").change(function(){
		 $("#uploadPic").blur().focus(); 
    });
	
	
	// Form validation rules/messages
	$("#upForm").validate({
		rules: {
			f: {
				required: true,
				regex: "(\.jpg$)"
			}
		},
		messages: {
			f: {
				required: "please select a file",
				regex: ".jpg only"
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

