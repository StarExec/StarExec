

$(document).ready(function(){
	attachFormValidation();

	initUI();
		
});

function initUI(){
	
	$("#btnDone").button({
		icons: {
			primary: "ui-icon-locked"
		}
	});

}


function attachFormValidation() {
	
	// Add regular expression capabilities to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
		
	// Set up form validation
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: $("#txtQueueName").attr("length"),
				regex : getPrimNameRegex()
			}
		},
		messages: {
			name:{
				required: "enter a queue name",
				minlength: "2 characters minimum",
				maxlength: $("#txtQueueName").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			}
		}
	});
};







/*
$(document).ready(function() {
	attachFormValidation();
	attachButtonActions();
	
});

function attachButtonActions() {

	/*
	$("#btnDone").button({
		icons: {
			secondary: "ui-icon-circle-check"
		}
	});
	
	
	$( "#btnDone" ).button({ 
		icons: { 
			primary: "ui-icon-gear", 
			secondary: "ui-icon-triangle-1-s" 
		} 
	});

	
	
	$("#btnBack").button({
		icons: {
			primary: "ui-icon-circle-check"
		}
	});
	
	$("#btnBack").click(function(){
		alert("inside click");
		history.back(-1);
	});

	
	
	$("#btnAdd").click(function() {
		var tbl = $("#tblConfig");
		alert("tbl = " + tbl);
		$("<td class='label'><p>Node</p></td><td>'SelectSomething'</td>").appendTo(tbl);        
	        
	    $(document.body).delegate(".delRowBtn", "click", function(){
	        $(this).closest("tr").remove();        
	    });    
	    
	});
	
}



*/

	





