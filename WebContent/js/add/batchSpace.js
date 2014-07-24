$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Initialize user-interface buttons/actions
 */
function initUI(){
	$('#btnUpload').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-n"
    }});
	
	$("#viewSchema").button({
		icons: {
			secondary: "ui-icon-document"
	}
	});
	
	$("#viewExample").button({
		icons: {
			secondary: "ui-icon-document"
	}
	});
	
}


/**
 * Attaches form validation to the file uploading field
 */
function attachFormValidation(){
	
	// Adds regular expression handling to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Re-validate the 'file location' field when it loses focus
	$("#fileUpload").change(function(){
		 $("#fileUpload").blur().focus(); 
    });
	
	// Form validation rules/messages
	$("#upForm").validate({
		rules: {
			f: {
				required: true,
				regex: "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			}
		},
		messages: {
			f: {
				required: "please select a file",
				regex: ".zip, .tar and .tar.gz only"
			}
		},
		submitHandler: function(form) {
			createDialog("Uploading XML, please wait. This will take some time for large files.");
			form.submit();
		}
	});
}