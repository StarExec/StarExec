var defaultPPId;
$(document).ready(function(){
	initUI();
	attachFormValidation();
	defaultPPId=$("#ppid").attr("value");
});


/**
 * Initializes the user-interface
 */
function initUI(){
	
	// Attach icons
	$('#cancel').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	$('#delete').button({
		icons: {
			secondary: "ui-icon-trash"
		}
	});
	
	$("fieldset:not(:first)").expandable(true);
	
	$("#cancel").click(function() {
		window.location=href=starexecRoot+"secure/edit/community.jsp?cid="+$("#cid").attr("value");
	});
	
	// Updates the database to reflect the newly inputed processor details
	// when the 'update' button is pressed 
	
	$("#update").click(function(){
		var isFormValid = $("#editProcForm").valid();
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			$.post(
					starexecRoot+"services/edit/processor/" + getParameterByName("id"),
					{ name: name, desc: description},
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");

						}

					},
					"json"
			);
		}
	});
	
	$("#delete").click(function(){
		var isFormValid = $("#editProcForm").valid();
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			var cid=$("#cid").attr("value");
			
			$.post(
					starexecRoot+"services/delete/processor/" + getParameterByName("id"),
					{},
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");
						}

					},
					"json"
			);
		}
	});
}

/**
 * Attaches form validation to the 'edit processor' fields
 */
function attachFormValidation(){
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editProcForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds regular expression 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Form validation rules/messages
	$("#editProcForm").validate({
		rules : {
			name : {
				required : true,
				maxlength: $("#name").attr("maxlength"),
				regex 	 : getPrimNameRegex()
			},
			description : {
				required : true,
				maxlength: $("#description").attr("length"),
				regex	 : getPrimDescRegex()
			}
			
		},
		messages : {
			name : {
				required : "name required",
				maxlength: $("#name").attr("maxlength") + " characters maximum",
				regex 	 : "invalid character(s)"
			},
			description : {
				required : "description required",
				maxlength: $("#description").attr("length") + " characters maximum",
				regex 	 : "invalid character(s)"
			}
		}
	});
}