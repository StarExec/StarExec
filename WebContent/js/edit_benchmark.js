$(document).ready(function(){
	$('table tr:even').addClass('shade');	
	
	
	// Prompts user to confirm deletion and, if they confirm, 
	// deletes the benchmark via AJAX and redirects to space_explorer.jsp
	$("#delete").click(function(){
		var confirm = window.confirm("are you sure you want to delete this benchmark?");
		if(confirm == true){
			$.post(
					"/starexec/services/delete/benchmark/" + getParameterByName("id"),
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/space_explorer.jsp';
								break;
							case 1:
								showMessage('error', "benchmark was not deleted; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this benchmark can modify its details", 5000);
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
	

	// Triggers validation and, if that passes,
	// updates the benchmark details via AJAX and redirects to benchmark.jsp 
	$("#update").click(function(){
		var isFormValid = $("#editBenchmarkForm").valid();
		if(isFormValid == true){
			var name = $("#name").val();
			var description = $("#description").val();
			var isDownloadable = $("#downloadable").is(':checked');
			var type = $("#benchType").val();
			var data = {name: name, description: description, downloadable: isDownloadable, type: type};
			$.post(
					"/starexec/services/edit/benchmark/" + getParameterByName("id"),
					data,
					function(returnCode) {
						switch (returnCode) {
							case 0:
								window.location = '/starexec/secure/details/benchmark.jsp?id=' + getParameterByName("id");
								break;
							case 1:
								showMessage('error', "benchmark details were not updated; please try again", 5000);
								break;
							case 2:
								showMessage('error', "only the owner of this benchmark can modify its details", 5000);
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
	
	
	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editBenchmarkForm").submit(function(e){
		e.preventDefault();
	});
	
	// Adds 'regex' function to validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Validates benchmark fields
	$("#editBenchmarkForm").validate({
		rules : {
			name : {
				required : true,
				regex : "^[a-zA-Z0-9\\-_\\.]+$"
			},
			description : {
				required : true
			}
		},
		messages : {
			name : {
				required : "name required",
				regex : "invalid characters"
			},
			description : {
				required : "description required"
			}
		}
	});
});


