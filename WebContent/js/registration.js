/**
 * 
 */

$(document).ready(function(){
	
	$("#regForm").validate({
		rules: {
			firstname: {
				required: true,
				minlength: 2
			},
			lastname: {
				required: true,
				minlength: 2
			},
			email: {
				required: true,
				email: true
			},
			institute:{
				required: true,
				minlength: 2
			},
			confirm_password: {
				required: true,
				equalTo: "#password"
			}
		},
		messages: {
			firstname:{
				required: "enter a first name",
				minlength: "needs to be at least 2 characters"
			},
			lastname: {
				required: "enter a last name",
				minlength: "needs to be at least 2 characters"
			},
			institute:{
				required: "enter your institution's name",
				minlength: "needs to be at least 2 characters"
			},
			confirm_password: {
				required: "please provide a password",
				equalTo: "passwords don't match"
			},
			email: {
				required: "enter a valid email address",
				email: "invalid email format"
			}
		}
		// the errorPlacement has to take the table layout into account
//		errorPlacement: function(error, element) {
//			error.prependTo( element.parent().next() );
//		}
	});
	
	
	
});