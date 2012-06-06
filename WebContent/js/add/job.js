// Variables for keeping state in the 3-step process of job creation
var progress = 0;
var defaultPPId = 0;
var solverUndo = [];
var benchUndo = [];

// When the document is ready to be executed on
$(document).ready(function(){
	// Add regex capabilities to validator
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
				regex : "^[\\w\\-\\. ]{1,32}$"
			},
			desc: {
				maxlength: 512,
				regex: "^[\\w\\]\\[!\"#\\$%&'()\\*\\+,\\./:;=\\?@\\^_`{\\|}~\\- ]{2,512}$"
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
				required: "enter a space name",
				minlength: ">= 2 characters",
				maxlength: "< 32 characters",
				regex: "invalid character(s)"
			},
			desc: {
				maxlength: "< 512 characters",
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
		}
	});

	// Initialize UI
	initUI();	
	
	$('#addForm').submit(function() {
		// Remove all unselected rows from the DOM before submitting
		$('#tblBenchConfig tbody').children('tr').not('.row_selected').find('input').remove();
		$('#tblSolverConfig tbody').children('tr').not('.row_selected').find('select, input').remove();
	  	return true;
	});
	
});


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
    }}).click(function(){
    	progress--;
    	updateProgress();
    });
    
    $('#btnDone').button({
		icons: {
			secondary: "ui-icon-check"
    }}).click(function(){
    	// Make sure the user has at least one bechmark in the table
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