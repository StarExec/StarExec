$(document).ready(function() {

	// Shade even rows of the table
	$('#inviteForm tr:even').addClass('shade');
	
	// Set the default text
	$("#reason").data('default', "Describe your motivation for joining this community and/or what you plan on using it for...");

	
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
	
	// Don't permit the defealt text as a message to the leaders by clearing it
	$("#submit").click(function(){
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
			}
		},
		// the errorPlacement ignores #password
		errorPlacement : function(error, element) {
			if($(element).attr("id") != "reason"){
				error.insertAfter(element);
			}
		}
	});

});

