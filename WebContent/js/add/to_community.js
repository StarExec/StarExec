$(document).ready(function() {
	// Set the default text
	$("#reason").data('default', "describe your motivation for joining this community");

	
	// Clear the textarea when clicked on if the text in there == the default text
	// and re-insert that default text if the user doesn't input anything
	$("#reason")
	  .focus(function() {
	        if (this.value === this.defaultValue) {
	            this.value = '';
	        }
	  })
	  .blur(function() {
	        if (this.value === '') {
	            this.value = this.defaultValue;
	        }
	});
	
	// Don't permit the default text as a message to the leaders
	$("#btnSubmit").click(function(){
		if ($("#reason").val() == $("#reason").data('default')){
			$("#reason").val("");
	    }
	});
	
	
	$("#inviteForm").validate({
		rules : {
			cm: {
				required: true
			},
			msg: {
				required: true
			}
		},
		messages : {
			cm : {
				required : "select a community to join"
			},
			msg: {
				required : "enter your reason for joining"
			}
		}
	});
	
	$('#btnSubmit').button({
		icons: {
			secondary: "ui-icon-mail-closed"
    }});
});

