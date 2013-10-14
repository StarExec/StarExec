var progress = 0;


$(document).ready(function() {
	attachFormValidation();
	
	$('#btnSubmit').button({
		icons: {
			secondary: "ui-icon-circle-check"
		}
	});
	
});


function attachFormValidation() {
	// Add regular expression capabilities to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	
	$.validator.addMethod(
			"greaterThan", 
			function(value, element, params) {

			    if (!/Invalid|NaN/.test(new Date(Date.parse(value)))) {
			        return new Date(Date.parse(value)) > new Date(Date.parse($(params).val()));
			    }
			    alert(Date.parse(value));
			    return isNaN(value) && isNaN($(params).val()) 
			        || (Number(value) > Number($(params).val())); 
			},'Must be greater than {0}.');
	
	$.validator.addMethod(
			"greaterThanToday", 
			function(value, element, params) {
				var today = new Date();
			    if (!/Invalid|NaN/.test(new Date())) {
			        return today <= new Date(Date.parse(value));
			    }
			    alert(today);
			    return isNaN(value) && isNaN(value) 
			        || (Number(value) > Number(value)); 
			},'Must be greater than {0}.');
	
	
	// Set up form validation
	var today = new Date();
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: $("#txtQueueName").attr("length"),
				regex : getPrimNameRegex()
			},
			node: {
				required: true,
				regex: "[0-9]+"
			},
			start: {
				required: true,
				greaterThanToday: today,
				regex:  "[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]"
			},
			end: {
				required: true,
				greaterThan: "#start",
				regex:  "[0-9][0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]"
			}
		},
		messages: {
			name:{
				required: "enter a queue name",
				minlength: "2 characters minimum",
				maxlength: $("#txtQueueName").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			node: {
				required: "enter a node count",
				regex: "invalid character(s)"
			},
			start: {
				required: "enter a start date",
				greaterThanToday: "date must be after or on today's date",
				regex: "invalid format - ex. mm/dd/yyyy"
			},
			end: {
				required: "enter a end date",
				greaterThan: "must be greater than start date",
				regex: "invalid format - ex. mm/dd/yyyy"
			}
		}
	});
};

	





