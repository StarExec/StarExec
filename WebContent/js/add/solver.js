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
 * Attaches form validation to the solver upload form
 */
function attachFormValidation(){
	
	// Add regular expression handler to jQuery validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Re-validate the 'solver location' field when it loses focus
	$("#fileLoc").change(function(){
		 $("#fileLoc").blur().focus(); 
    });
	
	// Form validation rules/messages	
	$("#upForm").validate({
		rules: {
			f: {
				required : true,
				regex	 : "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			},
			sn: {
				required : true,
				maxlength: 64,
				regex 	 : getPrimNameRegex()
			},
			desc: {
				required: false,
				maxlength: 1024,
				regex    : getPrimDescRegex()
			},
			d: {
			    required : false,
				regex	 : "(\.txt$)"
			},
		},
		messages: {
			f: {
				required: "please select a file",
				regex 	: ".zip, .tar and .tar.gz only"
			},
			sn: {
				required: "solver name required",
				maxlength: "64 characters maximum",
				regex 	: "invalid character(s)"
			},
			desc: {
				maxlength: "1024 characters maximum",
				regex: "invalid character(s)"
			},
			d: {
				regex: ".txt file only",
			}
		}
	});
}