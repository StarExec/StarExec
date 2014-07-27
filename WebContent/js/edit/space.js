$(document).ready(function(){
	initUI();
	attachFormValidation();
	attachButtonActions();
});

/**
 * Initializes the user-interface
 */
function initUI(){
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	
	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}}).click(function(){
		history.back(-1);
	});
}

/**
 * Attaches form validation to the 'edit space' fields
 */
function attachFormValidation(){
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editSpaceForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds regular expression handling to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Form validation rules/messages
	$("#editSpaceForm").validate({
		rules: {
			name: {
				required : true,
				maxlength: $("#name").attr("length"),
				regex	 : getPrimNameRegex()
			},
			description: {
				required : false,
				minlength: 0,
				maxlength: $("#description").attr("length")
				//regex	 : getPrimDescRegex()
			}
		},
		messages: {
			name:{
				required : "enter a space name",
				maxlength: $("#name").attr("length")+" characters maximum",
				regex	 : "invalid character(s)"
			},
			description: {
				required : "enter a description",
				maxlength: $("#description").attr("length")+" characters maximum"
				//regex	 : "invalid character(s)"
			}
		}
	});
}


/**
 * Attaches an action to the 'update' button
 */
function attachButtonActions(){
	// Triggers validation and, if that passes,
	// updates the space details via AJAX, then redirects to explore/spaces.jsp
	$("#update").click(function(){
		var isFormValid = $("#editSpaceForm").valid();
		if(isFormValid == true){
			// Extract relevant data from page
			var data = 
			{		name		: $("#name").val(), 
					description	: $("#description").val(),
					locked		: $("#locked").is(':checked'),
					sticky 		: $("#sticky").is(":checked"),
					addBench	: $("#addBench").is(':checked'),
					addJob		: $("#addJob").is(':checked'),
					addSolver	: $("#addSolver").is(':checked'),
					addSpace	: $("#addSpace").is(':checked'),
					addUser		: $("#addUser").is(':checked'),
					removeBench	: $("#removeBench").is(':checked'),
					removeJob	: $("#removeJob").is(':checked'),
					removeSolver: $("#removeSolver").is(':checked'),
					removeSpace	: $("#removeSpace").is(':checked'),
					removeUser	: $("#removeUser").is(':checked')
			};
			
			// Pass data to server via AJAX
			$.post(
					starexecRoot+"services/edit/space/" + getParameterByName("id"),
					data,
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/explore/spaces.jsp';
						}
					},
					"json"
			);
		}
	});
	
}