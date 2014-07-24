$(document).ready(function(){
	initUI();
	attachFormValidation();
	attachButtonActions();
});


/**
 * Initializes the user-interface
 */
function initUI(){

	
	// Setup JQuery button icons

	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
    }});

}


function attachFormValidation(){
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editQueueForm").submit(function(e){
		e.preventDefault();
	});

	
	// Form validation rules/messages
	$("#editQueueForm").validate({
		rules : {
			cpuTimeout : {
				required : true,
				min		 : 1
			},
			wallTimeout : {
				required : true,
				min		 : 1
			}
		},
		messages : {
			cpuTimeout : {
				required : "timeout required",
				min      : "minimum of 1 second timeout"
			},
			wallTimeout : {
				required : "timeout required",
				min      : "minimum of 1 second timeout"
			}
		}
	});
}

/**
 * Attaches the action for updating the queue
 */
function attachButtonActions(){
	$("#update").click(function(){
		var isFormValid = $("#editQueueForm").valid();
		if(isFormValid == true){
				//Extract Relevant Data from Page
				var data = 
				{
						cpuTimeout		: $("#cpuTimeout").val(), 
						wallTimeout	: $("#wallTimeout").val() 
				};
				//Pass data to server via AJAX
				$.post(
						starexecRoot+"services/edit/queue/" + getParameterByName("id"),
						data,
						function(returnCode) {
							switch (returnCode) {
								case 0:
									window.location = starexecRoot+'secure/admin/cluster.jsp';
									break;
								case 1:
									showMessage('error', "queue details were not updated; please try again", 5000);
									break;
								case 2:
									showMessage('error', "only the admin can modify queues", 5000);
									break;
								default:
									showMessage('error', "Invalid Parameters", 5000);
									break;
							}
						},
						"json"
				);	
		}
	});
	
}


