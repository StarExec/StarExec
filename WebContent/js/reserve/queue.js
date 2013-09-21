$(document).ready(function() {
	attachFormValidation();
	var i;
	$(function(){
	    var $select = $(".numList");
	    for (i=1;i<=100;i++){
	        $select.append($('<option></option>').val(i).html(i));
	    }
	});
	
	$('#btnSubmit').button({
		icons: {
			secondary: "ui-icon-mail-closed"
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
	
	
	// Set up form validation
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: $("#txtQueueName").attr("length"),
				regex : getPrimNameRegex()
			},
			msg: {
				required : true,
				minlength: 2,
				maxlength: 512,
				regex	 : getPrimDescRegex()
			},
			node: {
				required: true,
				regex: "[0-9]+"
			},
			start: {
				required: true,
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
			msg: {
				required: "enter your reason for reserving a queue",
				minlength: "2 characters minimum",
				maxlength: "512 charactes maximum",
				regex	 : "invalid character(s)"
			},
			node: {
				required: "enter a node name",
				regex: "invalid character(s)"
			},
			start: {
				required: "enter a start date",
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

