$(document).ready(function(){
	initUI();
	attachFormValidation();
	
	$("#radioLocal").attr("checked", "checked");
	$("#fileLoc").show();
	$("#fileURL").hide();
	
	$("#radioUpload").attr("checked", "checked");
	$("#default").show();	
	$("#fileLoc2").hide();
	$("#description").hide();
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
	
	
	$("#radioLocal").change(function() {
		if ($("#radioLocal").is(":checked")) {
			$("#fileURL").fadeOut('fast', function() {
				$("#fileLoc").fadeIn('fast');
			});
			
			
		}
	});

	$("#radioURL").change(function() {
		if ($("#radioURL").is(":checked")) {
			$("#fileLoc").fadeOut('fast', function() {
				$("#fileURL").fadeIn('fast');
			});
			
		}
	});
	
	$("#radioUpload").change(function() {
		if ($("#radioUpload").is(":checked")) {
			$("#description").fadeOut('fast', function() {
				$("#fileLoc2").fadeOut('fast', function() {
						$("#default").fadeIn('fast');
				});
			});
		}
	});
	
	$("#radioText").change(function() {
		if ($("#radioText").is(":checked")) {
			$("#fileLoc2").fadeOut('fast', function() {
				$("#default").fadeOut('fast',function() {
					$("#description").fadeIn('fast');
				});
			});
		}
	});
	
	$("#radioFile").change(function() {
		if ($("#radioFile").is(":checked")) {
			$("#description").fadeOut('fast', function() {
				$("#default").fadeOut('fast', function() {
					$("#fileLoc2").fadeIn('fast');
				});
			});
		}
	});
	
	
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
				required : "#radioLocal:checked",
				regex	 : "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			},
			url: {
				required : "#radioURL:checked",
				regex	 : "(\.tgz$)|(\.zip$)|(\.tar(\.gz)?$)"
			},
			sn: {
				required : true,
				maxlength: $("#name").attr("length"),
				regex 	 : getPrimNameRegex()
			},
			desc: {
				required: "#radioText:checked",
				maxlength: $("#description").attr("length"),
				regex    : getPrimDescRegex()
			},
			d: {
			    required : "#radioFile:checked",
				regex	 : "(\.txt$)"
			}
		},
		messages: {
			f: {
				required: "please select a file",
				regex 	: ".zip, .tar and .tar.gz only"
			},
			url: {
				required :"please enter a URL",
				regex	 :"URL must be .zip, .tar, or .tar.gz"	
			},
			sn: {
				required: "solver name required",
				maxlength: $("#name").attr("length") + " characters maximum",
				regex 	: "invalid character(s)"
			},
			desc: {
				required: "Description Required",
				maxlength: $("#description").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			d: {
				required: "Description Required",
				regex: ".txt file only"
			}
		}
	});
}