var progress = 0;


$(document).ready(function() {
	
	InitUI();
	attachFormValidation();
	
	editable("queuename");
	
});

function editable(attribute) {
	$('#edit' + attribute).click(function(){
		var old = $(this).html();
		$(this).after('<td><input type="text" name="name" value="' + old + '" />').remove();
	
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

function InitUI() {
	// Set up datatables
	$('#tblNodes').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',        
        "bPaginate": false,        
        "bSort": true        
    });
	
	// Place the select all/none buttons in the datatable footer
	$('#fieldSelectNode div.selectWrap').detach().prependTo('#fieldSelectNode div.bottom');
	
	var nodeCount = $("#nodeCount").attr("count");
	//highlight a certain number of rows at start
    $("#tblNodes tbody > tr:lt(" + nodeCount + ")").addClass('row_selected');
	
	$('#btnDecline').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	}).click(function(){
		$.post(
				starexecRoot+"services/cancel/request/" + getParameterByName("code"),
				function(returnCode) {
					switch (returnCode) {
						case 0:
							history.back(-1);
							//showMessage('success', "queue request was successfully declined", 3000);
							break;
						case 1:
							showMessage('error', "queue request was not declined; please try again", 5000);
							break;
						case 2:
							showMessage('error', "only the admin can decline a queue request", 5000);
							break;
						default:
							showMessage('error', "invalid parameters", 5000);
							break;
					}
				},
				"json"
		);
	});
    
    
	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
	}}).click(function(){
		
		history.back(-1);
	});
	
	$('#btnNext').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-e"
    }}).click(function(){
    	var isValid = $('#addForm').valid();
    	
    	// Make sure the job config form is valid  before moving on
    	if(progress == 0 && false == isValid) {
    		return;
    	}/* else if (progress == 1 && $('#tblNodes tbody tr.row_selected').length <= 0) { 
    		// Make sure the user has selected a choice for running the space
    		showMessage('warn', 'you must make a selection to continue', 3000);
    		return;
    	}
    	*/
    	// Move on to the next step if everything is valid
    	if (progress == 0){
    		progress++;    	   
    	}
    	updateProgress();
    });
	
	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function(){
		progress--;
    	updateProgress();
    });
    
    $('#btnDone').button({
		icons: {
			secondary: "ui-icon-check"
		}
    }).click(function(){
    	// Make sure the user has at least one benchmark in the table
    	if ((progress == 1 && $('#tblNodes tbody tr.row_selected').length <= 0)) {
    		showMessage('warn', 'you must have at least one node for this queue', 3000);
    		return false;
    	}
 		createDialog("Creating your queue, please wait.");
    });
    
    
    // Hook up select all/none buttons
    $('.selectAll').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
    });
    
    $('.selectNone').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
    });  
    
	$("#tblNodes").delegate("tr", "click", function() {
		$(this).toggleClass("row_selected");
	});
	
	updateProgress();

}

/**
 * Changes the UI to properly reflect what state the job creator is in
 */
function updateProgress() {

	// Hide all fields initially
	$('#fieldStep1').hide();
	$('#fieldSelectNode').hide();
	
	switch(progress) {
		case 0:	// Job setup stage
			$('#fieldStep1').fadeIn('fast');
			$('#btnNext').fadeIn('fast');
			$('#btnBack').fadeIn('fast');
			$('#btnPrev').fadeOut('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 1:	// Run space choice stage
			$('#fieldSelectNode').fadeIn('fast');
			$('#btnNext').fadeOut('fast');
			$('#btnBack').fadeOut('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeIn('fast');
			
			break;
	}
}

