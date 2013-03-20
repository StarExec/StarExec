// Variables for keeping state in the multi-step process of job creation
var progress = 0;
/*
0 = first page with basic job parameters
1 = choosing overall method - if the run space hierarchy method is chosen, then the rest are skipped and the job is run
2 = choose the method to select benchmarks
3 = choose the solvers and configurations from the space
4 = choose the benchmarks from the space  - whether 4 or 5 happens depends on choice in 2
5 = choose the benchmarks from the hierarchy - this is currently disabled so this step should never be reached.  some code is left
in case we find a need for this option and an efficient table for large #s of rows
*/
var defaultPPId = 0;
var solverUndo = [];
var benchUndo = [];

var benchMethodVal = 0;  //1 if choose from space, 2 if choose from hier, 0 otherwise

$(document).ready(function(){
	
	initUI();
	attachFormValidation();
	
	// Remove all unselected rows from the DOM before submitting
	$('#addForm').submit(function() {
		$('#tblBenchConfig tbody').children('tr').not('.row_selected').find('input').remove();
		//$('#tblBenchHier tbody').children('tr').not('.row_selected').find('input').remove();
		$('#tblSpaceSelection tbody').children('tr').not('.row_selected').find('input').remove();
		$('#tblSolverConfig tbody').children('tr').not('.row_selected').find('input').remove();
		$('#tblBenchMethodSelection tbody').children('tr').not('.row_selected').find('input').remove();
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
				required: false,
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
			},
			queue: {
				required: true
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
			},
			queue: {
				required: "error - no worker queues"
			}
		},
		// Place the error messages in the tooltip instead of in the DOM
		errorPlacement: function (error, element) {
			if($(error).text().length > 0){
				$(element).qtip('api').updateContent('<b>'+$(error).text()+'</b>', true);
			}
		},
		// Hide the error tooltip when no errors are present
		success: function(label){
			$('#' + $(label).attr('for')).qtip('api').hide();
		}
	});
};


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
	/*$('#fieldStep3 div.selectWrap').detach().prependTo('#fieldStep3 div.bottom');
	$('#fieldStep4 div.selectWrap').detach().prependTo('#fieldStep4 div.bottom');*/
	$('#fieldSelectBenchSpace div.selectWrap').detach().prependTo('#fieldSelectBenchSpace div.bottom');
	$('#fieldSolverSelection div.selectWrap').detach().prependTo('#fieldSolverSelection div.bottom');
	
	$('#btnNext').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-e"
    }}).click(function(){
    	var isValid = $('#addForm').valid();
    	
    	// Make sure the job config form is valid  before moving on
    	if(progress == 0 && false == isValid) {
    		return;
    	} else if (progress == 1 && $('#tblSpaceSelection tbody tr.row_selected').length <= 0) { 
    		// Make sure the user has selected a choice for running the space
    		showMessage('warn', 'you must make a selection to continue', 3000);
    		return;
    	}
    	else if (progress == 2 && $('#tblBenchMethodSelection tbody tr.row_selected').length <= 0) { 
        		// Make sure the user has selected a method for selecting benchmarks
        		showMessage('warn', 'you must make a selection to continue', 3000);
        		return;	
    	} else if (progress == 3 && $('#tblSolverConfig tbody tr.row_selected').length <= 0) {
    		// Make sure the user selects at least one solver before moving on
    		showMessage('warn', 'you must have at least one solver for this job', 3000);
    		return;
    	}
    	
    	// Move on to the next step if everything is valid
    	if (progress != 3){
    		progress++;    	   
    	}
    	else{
    		//if choosing bench from space
    		if (benchMethodVal == 1){
    			progress = 4;
    		}	
    		/*//if choosing bench from hierarchy
    		else if (benchMethodVal == 2){
    			progress = 5;
    		}
    		*/
    	}
    	updateProgress();
    });
	
	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function(){
    	//must go back two if choosing from hierarchy
		if (progress == 5){
    		progress = 3;
    	}
    	else{
			progress--;
    	}
    	updateProgress();
    });
    
    $('#btnDone').button({
		icons: {
			secondary: "ui-icon-check"
		}
    }).click(function(){
    	// Make sure the user has at least one benchmark in the table
    	if ((progress == 4 && $('#tblBenchConfig tbody tr.row_selected').length <= 0)) {
    		showMessage('warn', 'you must have at least one benchmark for this job', 3000);
    		return false;
    	}
    	 if (progress == 3 && $('#tblSolverConfig tbody tr.row_selected').length <= 0) {
     		// Make sure the user selects at least one solver before moving on
     		showMessage('warn', 'you must have at least one solver for this job', 3000);
     		return false;
    	 }
    });
    
    // Hook up select all/none buttons
    $('.selectAll').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
    	$(this).parents('.dataTables_wrapper').find('input').attr('checked', 'checked');
    });
    
    $('.selectNone').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').removeClass('row_selected');
    	$(this).parents('.dataTables_wrapper').find('input').removeAttr('checked');
    });  
    
    $('.selectDefault').click(function() {
    	$(this).parents('.dataTables_wrapper').find('tbody>tr').addClass('row_selected');
    	$(this).parents('.dataTables_wrapper').find('input').removeAttr('checked');
    	$(this).parents('.dataTables_wrapper').find('div>input:first-child').attr('checked', 'checked');
    });
    
    // Enable row selection
	$("#tblSolverConfig, #tblBenchConfig").delegate("tr", "click", function(){
		$(this).toggleClass("row_selected");
	});
	
	
	// Step 2 related actions
	// Selection toggling
	$("#tblSpaceSelection, #tblBenchMethodSelection").delegate("tr", "click", function(){
		$(this).addClass("row_selected");
		$(this).siblings().removeClass("row_selected");
	});
	
	
	// quick hierarchy run selected
	//$("#runSpace, #runHierarchy, #keepHierarchy").click(function() {
	$("#keepHierarchy").click(function() {
		$("#tblBenchConfig tr").addClass("row_selected");
		//$("#tblBenchHier tr").addClass("row_selected");
		$("#tblSolverConfig tr").addClass("row_selected");
    	$("#tblSolverConfig tr").find('input').attr('checked', 'checked');
		$('#btnNext').fadeOut('fast');
		$('#btnDone').fadeIn('fast');
	});
	
	// Choose benchmarks selected
	$("#runChoose").click(function() {
		$("#tblBenchConfig tr").removeClass("row_selected");
		//$("#tblBenchHier tr").removeClass("row_selected");
		$("#tblSolverConfig tr").removeClass("row_selected");
    	$("#tblSolverConfig tr").find('input').removeAttr('checked');
		$('#btnDone').fadeOut('fast');
		$('#btnNext').fadeIn('fast');
	});
	
	// Choose benchmarks in space selected
	$("#someBenchInSpace").click(function() {
		benchMethodVal = 1;
		$("#tblBenchConfig tr").removeClass("row_selected");
		//$("#tblBenchHier tr").removeClass("row_selected");
		$("#tblSolverConfig tr").removeClass("row_selected");
    	$("#tblSolverConfig tr").find('input').removeAttr('checked');
	});
	
	// Choose benchmarks in hierarchy selected
	/*$("#someBenchInHierarchy").click(function() {
		benchMethodVal = 2;
		$("#tblBenchConfig tr").removeClass("row_selected");
		//$("#tblBenchHier tr").removeClass("row_selected");
		$("#tblSolverConfig tr").removeClass("row_selected");
    	$("#tblSolverConfig tr").find('input').removeAttr('checked');
	});*/
	
	// all benchmarks in space 
	$("#allBenchInSpace").click(function() {
		benchMethodVal = 0;
		$("#tblBenchConfig tr").addClass("row_selected");
		//$("#tblBenchHier tr").removeClass("row_selected");
		$("#tblSolverConfig tr").removeClass("row_selected");
    	$("#tblSolverConfig tr").find('input').removeAttr('checked');
	});
	
	// all benchmarks in hierarchy 
	$("#allBenchInHierarchy").click(function() {
		benchMethodVal = 0;
		$("#tblBenchConfig tr").removeClass("row_selected");
		//$("#tblBenchHier tr").addClass("row_selected");
		$("#tblSolverConfig tr").removeClass("row_selected");
    	$("#tblSolverConfig tr").find('input').removeAttr('checked');
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
    $('#workerQueue').qtip(getErrorTooltip());
}

/**
 * Changes the UI to properly reflect what state the job creator is in
 */
function updateProgress() {

	// Hide all fields initially
	$('#fieldStep1').hide();
	/*$('#fieldStep2').hide();
	$('#fieldStep3').hide();
	$('#fieldStep4').hide();*/
	$('#fieldSolverMethod').hide();
	$('#fieldSolverSelection').hide();
	$('#fieldBenchMethod').hide();
	$('#fieldSelectBenchSpace').hide();
	//$('#fieldSelectBenchHierarchy').hide();
		
	switch(progress) {
		case 0:	// Job setup stage
			$('#fieldStep1').fadeIn('fast');
			$('#btnNext').fadeIn('fast');
			$('#btnPrev').fadeOut('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 1:	// Run space choice stage
			$('#fieldSolverMethod').fadeIn('fast');
			$('#btnNext').fadeOut('fast');
			$('#btnPrev').fadeIn('fast');
			break;
		case 2:	// If quick run space method not chosen, how to select benchmarks
			$('#fieldBenchMethod').fadeIn('fast');
			$('#btnNext').fadeIn('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 3:	// selecting solvers and configurations
			$('#fieldSolverSelection').fadeIn('fast');
			$('#btnPrev').fadeIn('fast');
			//still need to select benchmarks
			if (benchMethodVal > 0){
				$('#btnNext').fadeIn('fast');
				$('#btnDone').fadeOut('fast');
			}
			//benchmarks already selected
			else{
				$('#btnNext').fadeOut('fast');
				$('#btnDone').fadeIn('fast');
			}
			break;
		case 4:	// selecting benchmarks from the space
			$('#fieldSelectBenchSpace').fadeIn('fast');
			$('#btnNext').fadeOut('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeIn('fast');
			break;
		/*case 5:	// selecting benchmarks from the hierarchy - never reached
			$('#fieldSelectBenchHierarchy').fadeIn('fast');
			$('#btnNext').fadeOut('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeIn('fast');
			break;	*/
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
				type: 'fade',
				length: 200
			}
		},
		hide: {
			when: false,	// Don't tie the hiding of this to any event
			effect: {
				type: 'fade',
				length: 200
			}
		},
		style: {
			tip: 'leftMiddle',
			name: 'errorTooltip'
		},
		api:{
			onContentUpdate: function(){
				// Fixes the bug where sometimes opacity is < 1
				$('div[qtip="'+this.id+'"]').css('opacity',1);
			}
		}
	};
}