$(document).ready(function(){
	// Attach click listeners to both buttons
	initButtons();
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editSpaceForm").submit(function(e){
		e.preventDefault();
	});
	
	// Validates space fields
	$("#editSpaceForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: 32
			},
			desc: {
				maxlength: 1024
			}
		},
		messages: {
			name:{
				required: "enter a space name",
				minlength: ">= 2 characters",
				maxlength: "< 32 characters"
			},
			desc: {
				maxlength: "< 1024 characters"
			}
		}
	});
	
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
    }});
});

function initButtons(){
	// Triggers validation and, if that passes,
	// updates the space details via AJAX, then redirects to explore/spaces.jsp
	$("#update").click(function(){
		var isFormValid = $("#editSpaceForm").valid();
		if(isFormValid == true){
			var name = $("#name").val();
			var description = $("#description").val();
			var isLocked = $("#locked").is(':checked');
			var addBench = $("#addBench").is(':checked');
			var addJob = $("#addJob").is(':checked');
			var addSolver = $("#addSolver").is(':checked');
			var addSpace = $("#addSpace").is(':checked');
			var addUser = $("#addUser").is(':checked');
			var removeBench = $("#removeBench").is(':checked');
			var removeJob = $("#removeJob").is(':checked');
			var removeSolver = $("#removeSolver").is(':checked');
			var removeSpace = $("#removeSpace").is(':checked');
			var removeUser = $("#removeUser").is(':checked');
			var data = {name: name, description: description, locked: isLocked, addBench: addBench,
					addJob: addJob, addSolver: addSolver, addSpace: addSpace, addUser: addUser,
					removeBench: removeBench, removeJob: removeJob, removeSolver: removeSolver,
					removeSpace: removeSpace, removeUser: removeUser};
			
			$.post(
					"/starexec/services/edit/space/" + getParameterByName("id"),
					data,
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/explore/spaces.jsp';
								break;
							case 1:
								showMessage('error', "space details were not updated; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only a leader of this space can modify its details", 5000);
								break;
							default:
								showMessage('error', "invalid parameters", 5000);
								break;
						}
					},
					"json"
			);
		}
	});
	
}