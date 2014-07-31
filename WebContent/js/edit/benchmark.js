$(document).ready(function(){
	initUI();
	attachFormValidation();
	attachButtonActions();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	$('#delete').button({
		icons: {
			secondary: "ui-icon-minus"
		}
	});
	
	$('#update').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
	$('#dialog-confirm-delete').hide();
}


/**
 * Attaches form validation to the 'edit benchmark' fields
 */
function attachFormValidation(){
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editBenchmarkForm").submit(function(e){
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
	$("#editBenchmarkForm").validate({
		rules : {
			name : {
				required : true,
				maxlength: $("#name").attr("maxlength"),
				regex : getPrimNameRegex()
			},
			description : {
				required : true,
				maxlength: $("#description").attr("length"),
				regex: getPrimDescRegex()
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
				regex	 : "invalid character(s)"
			}
		}
	});
}

/**
 * Attaches actions to the 'update' and 'delete' buttons
 */
function attachButtonActions(){
	// Prompts user to confirm deletion and, if they confirm,
	// deletes the solver via AJAX, then redirects to explore/spaces.jsp
	$("#delete").click(function(){
		$('#dialog-confirm-delete-txt').text('are you sure you want to recycle this benchmark?');
		
		$('#dialog-confirm-delete').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					log('user confirmed benchmark deletion.');
					$('#dialog-confirm-delete').dialog('close');
					
					$.post(
							starexecRoot+"services/recycle/benchmark/" + getParameterByName("id"),
							function(returnCode) {
								s=parseReturnCode(returnCode);
								if (s) {
									window.location = starexecRoot+'secure/explore/spaces.jsp';
								}
							},
							"json"
					);
				},
				"cancel": function() {
					log('user canceled job deletion');
					$(this).dialog("close");
				}
			}
		});
	});
	

	// If the 'update' button is pressed then trigger validation and, if that passes,
	// update the benchmark details via AJAX and redirect to /details/benchmark.jsp 
	$("#update").click(function(){
		var isFormValid = $("#editBenchmarkForm").valid();
		if(isFormValid == true){
			var name = $("#name").val();
			var description = $("#description").val();
			var isDownloadable = $("#downloadable").is(':checked');
			var type = $("#benchType").val();
			var data = {name: name, description: description, downloadable: isDownloadable, type: type};
			$.post(
					starexecRoot+"services/edit/benchmark/" + getParameterByName("id"),
					data,
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/details/benchmark.jsp?id=' + getParameterByName("id");
						}

					},
					"json"
			);
		}
	});
}