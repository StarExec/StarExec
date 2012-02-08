// Variables for keeping state in the 3-step process of job creation
var progress = 0;
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
			queue: {
				required: true,
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
			queue: {
				required: "select a worker queue",
			}
		}
	});
	
	// Initialize buttons
	initButtons();
});

/**
 * Sets up the jQuery button style and attaches click handlers to those buttons.
 */
function initButtons() {
	$('#btnNext').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-e"
    }}).click(function(){
    	var isValid = $('#addForm').valid();
    	
    	// Make sure the job config form is valid  before moving on
    	if(progress == 0 && false == isValid) {
    		return;
    	} else if (progress == 1 && $('#tblSolverConfig tbody tr').length <= 1) {
    		// Make sure the user selects at least one solver before moving on
    		showMessage('warn', 'you must have at least one solver for this job', 2000);
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
	
	$('#btnUndo').button({
		icons: {
			secondary: "ui-icon-arrowrefresh-1-w"
    }}).click(function(){
    	switch(progress) {
	    	case 1:	// Solver config stage
	    		// Pop an element from the undo list and add it back to the table
	    		$('#tblSolverConfig tbody').append(solverUndo.pop());
	    		break;
	    	case 2:	// Benchmark config stage
	    		// Pop an element from the undo list and add it back to the table
	    		$('#tblBenchConfig tbody').append(benchUndo.pop());
	    		break;
    	}
    	
    	if(solverUndo.length < 1 && benchUndo.length < 1) {
    		// If there are no more items to undo, then hide the undo button
    		$('#btnUndo').fadeOut('fast');
    	}
    });
    
    $('#btnDone').button({
		icons: {
			secondary: "ui-icon-check"
    }}).click(function(){
    	// Make sure the user has at least one bechmark in the table
    	if (progress == 2 && $('#tblBenchConfig tbody tr').length <= 1) {
    		showMessage('warn', 'you must have at least one benchmark for this job', 2000);
    		return false;
    	}
    });
    
    // Initialize the state of the job creator by forcing a progress update
    updateProgress();
    
    // Initially hide the undo button
	$('#btnUndo').fadeOut('fast');
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
 * Removes a benchmak from a table and adds it to the undo list
 */
function removeBench(obj) {
	// Save the DOM object
	benchUndo.push($(obj).parent().parent());	
	// Remove it from DOM
	$(obj).parent().parent().remove();
	$('#btnUndo').fadeIn('fast');
}

/**
 * Removes a solver from a table and adds it to the undo list
 */
function removeSolver(obj) {
	// Save the DOM object
	solverUndo.push($(obj).parent().parent());
	// Remove it from DOM
	$(obj).parent().parent().remove();
	$('#btnUndo').fadeIn('fast');
}