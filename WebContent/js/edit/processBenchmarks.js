$(document).ready(function(){
	initUI();
});


/**
 * Initializes the user-interface
 */
function initUI(){
	
	// Attach icons
	$('#cancel').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	$('#process').button({
		icons: {
			secondary: "ui-icon-check"
		}
	});
		
	$("#cancel").click(function() {
		window.location=href=starexecRoot+"secure/explore/spaces.jsp";
	});
	
	// Updates the database to reflect the newly inputed processor details
	// when the 'update' button is pressed 
	// Remove all unselected rows from the DOM before submitting
	$('#processBenchForm').submit(function() {
		$('#processorSelectionTable tbody').children('tr').not('.row_selected').find('input').remove();
		createDialog("Processing your benchmarks, please wait. This will take some time for large numbers of benchmarks.");
	  	return true;
	});
	
	$("#processorSelectionTable tbody").on("mousedown", "tr", function(){
		if ($(this).hasClass("row_selected")) {
			$("#process").hide();
			$("#processorSelectionTable").find("tr").removeClass("row_selected");
		} else {
			$("#processorSelectionTable").find("tr").removeClass("row_selected");
			$(this).toggleClass("row_selected");
			$("#process").show();
		}
	});

	$("#process").click(function(){
		if(true == isFormValid){
			var name = $("#name").val();
			var description = $("#description").val();
			$.post(
					starexecRoot+"services/edit/processor/" + getParameterByName("id"),
					{ name: name, desc: description},
					function(returnCode) {
						s=parseReturnCode(returnCode);
						if (s) {
							window.location = starexecRoot+'secure/edit/community.jsp?cid=' + $("#cid").attr("value");
						}
					},
					"json"
			);
		}
	});
	$("#process").hide();
	
}

