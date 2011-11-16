$(document).ready(function() {

	// Shade even rows of the table
	$('#regForm tr:even').addClass('shade');

	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Set the default text
	$("#reason").data('default', "Describe your motivation for joining this community and/or what you plan on using it for...");

	// Clear the textarea when clicked on if the text in there == the default text
	// and re-insert that default text if the user doesn't input anything
	$("#reason")
	  .focus(function() {
	        if (this.value === this.defaultValue) {
	            this.value = '';
	        }
	  })
	  .blur(function() {
	        if (this.value === '') {
	            this.value = this.defaultValue;
	        }
	});
	
	// Don't permit the defealt text as a message to the leaders by clearing it
	$("#submit").click(function(){
		if ($("#reason").val() == $("#reason").data('default')){
			$("#reason").val("");
	    }
	});
	
	
	$("#regForm").validate({
		rules : {
			fn : {
				required : true,
				regex : "^[a-zA-Z\\-']+$",
				minlength : 2
			},
			ln : {
				required : true,
				regex : "^[a-zA-Z\\-']+$",
				minlength : 2
			},
			em : {
				required : true,
				email : true
			},
			inst : {
				required : true,
				regex : "^[a-zA-Z\\-\\s]+$",
				minlength : 2
			},
			confirm_password : {
				required : true,
				equalTo : "#password"
			},
			cm: {
				required: true
			},
			msg: {
				required: true
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
			inst : {
				required : "enter your institution's name",
				minlength : "needs to be at least 2 characters",
				regex : "invalid characters"
			},
			confirm_password : {
				required : "please provide a password",
				equalTo : "passwords don't match"
			},
			em : {
				required : "enter a valid email address",
				email : "invalid email format"
			},
			cm : {
				required : "select a community to join"
			}
		},
		// the errorPlacement ignores #password & #reason
		errorPlacement : function(error, element) {
			if($(element).attr("id") != "password" && $(element).attr("id") != "reason"){
				error.insertAfter(element);
			}
		}
	});

});

