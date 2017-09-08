jQuery(function($) {
	var $name = $("#name");
	var $description = $("#description");
	var $timeLimit = $("[name='timelimit']");
	var $syntax = $("[name='syntax']");
	var $editProcForm = $("#editProcForm");

	// Adds regular expression 'regex' function to validator
	$.validator.addMethod(
			"regex",
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
			}
	);

	// Pressing the enter key on an input field triggers a submit,
	// and this special validation process doesn't use submit, so
	// the following code prevents that trigger
	$("#editProcForm")
		.submit(function(e) {
			e.preventDefault();
		})
		// Form validation rules/messages
		.validate({
			rules : {
				name : {
					required : true,
					maxlength: $name.attr("maxlength"),
					regex    : getPrimNameRegex()
				},
				description : {
					required : true,
					maxlength: $description.attr("length"),
					regex    : getPrimDescRegex()
				}
			},
			messages : {
				name : {
					required : "name required",
					maxlength: $name.attr("maxlength") + " characters maximum",
					regex    : "invalid character(s)"
				},
				description : {
					required : "description required",
					maxlength: $description.attr("length") + " characters maximum",
					regex    : "invalid character(s)"
				}
			}
		})
	;

	$("fieldset:not(:first)").expandable(true);

	$('#cancel')
		.button({
			icons: {
				secondary: "ui-icon-closethick"
			}
		})
		.click(function() {
			window.location = starexecRoot+"secure/edit/community.jsp?cid="+$("#cid").attr("value");
		})
	;

	$('#update')
		.button({
			icons: {
				secondary: "ui-icon-check"
			}
		})
		// Updates the database to reflect the newly inputed processor details
		// when the 'update' button is pressed
		.click(function(){
			if ($editProcForm.valid()) {
				var data = {
					"name" : $name.val(),
					"desc" : $description.val(),
					"timelimit" : parseInt($timeLimit.val(), 10)
				};
				if ($syntax.length !== 0) {
					data["syntax"] = $syntax.val();
				}
				$.post(
					starexecRoot+"services/edit/processor/" + getParameterByName("id"),
					data,
					function(returnCode) {
						var s = parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");
						}
					},
					"json"
				);
			}
		})
	;

	$('#delete')
		.button({
			icons: {
				secondary: "ui-icon-trash"
			}
		})
		.click(function(){
			if ($editProcForm.valid()) {
				var name = $name.val();
				var description = $description.val();
				var cid = $("#cid").attr("value");
				var tempProcId = [getParameterByName("id")];
				$.post(
						starexecRoot+"services/delete/processor",
						{selectedIds: tempProcId},
						function(returnCode) {
							var s = parseReturnCode(returnCode);
							if (s) {
								window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");
							}
						},
						"json"
				);
			}
		})
	;
});
