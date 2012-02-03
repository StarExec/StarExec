$(document).ready(function() {
	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});

	
	// Validate fields
	$("#resetForm").validate({
		rules : {
			fn : {
				required : true,
				regex : "^[a-zA-Z\\-'\\s]+$",
				minlength : 2
			},
			ln : {
				required : true,
				regex : "^[a-zA-Z\\-'\\s]+$",
				minlength : 2
			},
			em : {
				required : true,
				email : true
			}			
		},
		messages : {
			fn : {
				required : "enter a first name",
				minlength : "needs to be at least 2 characters",
				regex : "invalid characters"
			},
			ln : {
				required : "enter a last name",
				minlength : "needs to be at least 2 characters",
				regex : "invalid characters"
			},
			em : {
				required : "enter a valid email address",
				email : "invalid email format"
			}
		}
	});

	$('#submit').button({
		icons: {
			secondary: "ui-icon-arrowrefresh-1-s"
    }});
});