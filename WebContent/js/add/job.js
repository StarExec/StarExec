// Variables for keeping state in the 3-step process of job creation
var progress = 0;
var defaultPPId = 0;
var solverUndo = [];
var benchUndo = [];

$(document).ready(function(){
	
	initUI();
	attachFormValidation();
	
	// Remove all unselected rows from the DOM before submitting
	$('#addForm').submit(function() {
		$('#tblBenchConfig tbody').children('tr').not('.row_selected').find('input').remove();
		$('#tblSolverConfig tbody').children('tr').not('.row_selected').find('select, input').remove();
	  	return true;
	});
	
});


/**
 * Attach validation to the job creation form
 */
function attachFormValidation(){
	
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
				maxlength: 32,
				regex : getPrimNameRegex()
			},
			desc: {
				required: true,
				maxlength: 1024,
				regex: getPrimDescRegex()
			},
			cpuTimeout: {
				required: true,			    
			    max: 259200
			},
			wallclockTimeout: {
				required: true,			    
			    max: 259200
			}
		},
		messages: {
			name:{
				required: "enter a job name",
				minlength: "2 characters minimum",
				maxlength: "32 characters maximum",
				regex: "invalid character(s)"
			},
			desc: {
				required: "enter a job description",
				maxlength: "1024 characters maximum",
				regex: "invalid character(s)"
			},
			cpuTimeout: {
				required: "enter a timeout",			    
			    max: "3 day max timeout"
			},
			wallclockTimeout: {
				required: "enter a timeout",			    
			    max: "3 day max timeout"
			}
		},
		// Place the error messages in the tooltip instead of in the DOM
		errorPlacement: function (error, element) {
			$(element).qtip('api').updateContent('<b>'+$(error).text()+'</b>', true);
		},
		// Hide the error tooltip when no errors are present
		success: function(label){
			$('#' + $(label).attr('for')).qtip('api').hide();
		}
	});
}

/**
 * Sets up the jQuery button style and attaches click handlers to those buttons.
 */
function initUI() {
	
	// Set the selected post processor to be the default one
	defaultPPId = $('#postProcess').attr('default');
	$('#postProcess option[value=' + defaultPPId + ']').attr('selected', 'selected');
	
	// Set up datatables
	$('#tblSolverConfig, #tblBenchConfig').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',        
        "bPaginate": false,        
        "bSort": true        
    });
	
	// Place the select all/none buttons in the datatable footer
	$('#fieldStep2 div.selectWrap').detach().prependTo('#fieldStep2 div.bottom');
	$('#fieldStep3 div.selectWrap').detach().prependTo('#fieldStep3 div.bottom');
	
	$('#btnNext').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-e"
    }}).click(function(){
    	var isValid = $('#addForm').valid();
    	
    	// Make sure the job config form is valid  before moving on
    	if(progress == 0 && false == isValid) {
    		return;
    	} else if (progress == 1 && $('#tblSolverConfig tbody tr.row_selected').length <= 0) {    	
    		// Make sure the user selects at least one solver before moving on
    		showMessage('warn', 'you must have at least one solver for this job', 3000);
    		return;
    	}
    	
    	// Move on to the next step if everything is valid
    	progress++;    	    
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
    	if (progress == 2 && $('#tblBenchConfig tbody tr.row_selected').length <= 0) {
    		showMessage('warn', 'you must have at least one benchmark for this job', 3000);
    		return false;
    	}
    });
    
    // Hook up select all/none buttons
    $('.selectAll').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
    });
    
    $('.selectNone').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
    });
    
    // Enable row selection
	$("#tblSolverConfig, #tblBenchConfig").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
    
	// Set timeout default to 1 day	
	$("#timeoutDay option[value='1']").attr("selected", "selected");
	
    // Initialize the state of the job creator by forcing a progress update
    updateProgress();           
    
    // Have the validation errors appear in tooltips instead of plain text
    $('#txtJobName').qtip(getErrorTooltip());
    $('#txtDesc').qtip(getErrorTooltip());
    $('#wallclockTimeout').qtip(getErrorTooltip());
    $('#cpuTimeout').qtip(getErrorTooltip());
}

/**
 * Changes the UI to properly reflect what state the job creator is in
 */
function updateProgress() {
	// Hide all fields initially
	$('#fieldStep1').hide();
	$('#fieldStep2').hide();
	$('#fieldStep3').hide();
		
	switch(progress) {
		case 0:	// Job setup stage
			$('#fieldStep1').fadeIn('fast');
			$('#btnNext').fadeIn('fast');
			$('#btnPrev').fadeOut('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 1:	// Solver config stage
			$('#fieldStep2').fadeIn('fast');
			$('#btnNext').fadeIn('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 2:	// Bench config stage
			$('#fieldStep3').fadeIn('fast');
			$('#btnNext').fadeOut('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeIn('fast');
			break;
	}
}



/**
 * Returns the tooltip configuration used to display error messages to the client
 */
function getErrorTooltip(){
	// Sets up the tooltip look & feel
	$.fn.qtip.styles.errorTooltip = {
			background: '#E1E1E1',
			'padding-left': 15,
			'padding-right': 8,
			'padding-top': 8,
			'padding-bottom': 8,
			color : '#ae0000'
	};
	
	// Return the tooltip configuration using the above style
	return {
		position: {
			corner:{
				target: 'rightMiddle',
				tooltip: 'leftMiddle'
			}
		},
		show: {
			when: false,	// Don't tie the showing of this to any event
			ready: false,	// Don't display tooltip once it has been initialized
			effect: {
				type: 'grow'
			}
		},
		hide: {
			when: false,	// Don't tie the hiding of this to any event
			effect: 'fade'
		},
		style: {
			tip: 'leftMiddle',
			name: 'errorTooltip'
		},
		api:{
			onContentUpdate: function(){
				// Hide the tooltip initially by not showing the tooltip when it's empty
				if($(this.elements.content).text().length > 1){
					this.show();
					// Fixes the bug where sometimes opacity is < 1
					$('div[qtip="'+this.id+'"]').css('opacity',1);
				}
			}
		}
	};
}