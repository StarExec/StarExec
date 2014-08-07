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
	
	$('#radioDepth').attr('checked','checked');
	$('#radioNoPause').attr('checked','checked');

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

function getMaxCpuTimeout(){
	maxtime=$( "#workerQueue option:selected" ).attr("cpumax");
	return parseInt(maxtime);
}

function getMaxWallTimeout() {
	maxtime=$( "#workerQueue option:selected" ).attr("wallmax");
	return parseInt(maxtime);
}

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
				maxlength: $("#txtJobName").attr("length"),
				regex : getPrimNameRegex()
			},
			desc: {
				required: false,
				maxlength: $("#txtDesc").attr("length"),
				regex: getPrimDescRegex()
			},
			cpuTimeout: {
				required: true,			    
			    max: getMaxCpuTimeout(),
			    min: 1
			},
			wallclockTimeout: {
				required: true,			    
			    max: getMaxWallTimeout(),
			    min: 1
			},
			maxMem: {
				required: true,
				min : 0 
			},
			queue: {
				required: true
			}
		},
		messages: {
			name:{
				required: "enter a job name",
				minlength: "2 characters minimum",
				maxlength: $("#txtJobName").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			desc: {
				required: "enter a job description",
				maxlength: $("#txtDesc").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			cpuTimeout: {
				required: "enter a timeout",			    
			    max: getMaxCpuTimeout()+" second max timeout",
			    min: "1 second minimum timeout"
			},
			wallclockTimeout: {
				required: "enter a timeout",			    
			    max: getMaxWallTimeout()+" second max timeout",
			    min: "1 second minimum timeout"
			},
			maxMem: {
				required: "enter a maximum memory",
				max: "100 gigabytes maximum" 
			},
			queue: {
				required: "error - no worker queues"
			}
		}
	});
	
	//when we change queues, we need to refresh the validation to use the new timeouts
	$("#workerQueue").change(function() {
		settings = $('#addForm').validate().settings;
		settings.rules.cpuTimeout = {
				required: true,			    
			    max: getMaxCpuTimeout(),
			    min: 1
			};
		
		settings.rules.wallclockTimeout = {
				required: true,			    
			    max: getMaxWallTimeout(),
			    min: 1
			};
		
		settings.messages.cpuTimeout = {
				required: "enter a timeout",			    
			    max: getMaxCpuTimeout()+" second max timeout",
			    min: "1 second minimum timeout"
			};
		
		settings.messages.wallclockTimeout = {
				required: "enter a timeout",			    
			    max: getMaxWallTimeout()+" second max timeout",
			    min: "1 second minimum timeout"
		};
		$("#addForm").valid(); //revalidate now that we have new rules

		
	});
};


/**
 * Sets up the jQuery button style and attaches click handlers to those buttons.
 */
function initUI() {
	
	// Set the selected post processor to be the default one
	
	defaultPPId = $('#postProcess').attr('default');
	if (stringExists(defaultPPId)) {
		$('#postProcess option[value=' + defaultPPId + ']').attr('selected', 'selected');
	}
	
	//If there is only one post processor and for some reason it is not the default, set it as such
	if ($("#postProcess").find("option").length==2) {
		$("#postProcess").find("option").last().attr("selected","selected");
	}
	
	defaultPPId = $('#preProcess').attr('default');
	if (stringExists(defaultPPId)) {
		$('#preProcess option[value=' + defaultPPId + ']').attr('selected', 'selected');
	}
	
	//If there is only one pre processor and for some reason it is not the default, set it as such
	if ($("#preProcess").find("option").length==2) {
		$("#preProcess").find("option").last().attr("selected","selected");
	}
	
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
    	} else if (progress == 1 && $('#tblSpaceSelection tbody tr.row_selected').length <= 0) { 
    		// Make sure the user has selected a choice for running the space
    		showMessage('warn', 'You must make a selection to continue.', 3000);
    		return;
    	}
    	else if (progress == 2 && $('#tblBenchMethodSelection tbody tr.row_selected').length <= 0) { 
        		// Make sure the user has selected a method for selecting benchmarks
        		showMessage('warn', 'You must make a selection to continue.', 3000);
        		return;	
    	} else if (progress == 3 && $('#tblSolverConfig tbody tr.row_selected').length <= 0) {
    		// Make sure the user selects at least one solver before moving on
    		showMessage('warn', 'You must have at least one solver for this job.', 3000);
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
    		showMessage('warn', 'You must have at least one benchmark for this job.', 3000);
    		return false;
    	}
    	 if (progress == 3 && $('#tblSolverConfig tbody tr.row_selected').length <= 0) {
     		// Make sure the user selects at least one solver before moving on
     		showMessage('warn', 'You must have at least one solver for this job.', 3000);
     		return false;
    	 }
 		createDialog("Creating your job, please wait. This will take some time for large jobs.");
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
	$("#tblSolverConfig").on( "click", "tr", function(){
		if ( $(this).find('div>input').is(':checked')) {
			$(this).addClass("row_selected");
		}; 
		if ( !$(this).find('div>input').is(':checked')) {
			$(this).removeClass("row_selected");
		};
	});
	
	$("#tblBenchConfig").on( "click","tr", function() {
		$(this).toggleClass("row_selected");
	});


	// Step 2 related actions
	// Selection toggling
	$("#tblSpaceSelection, #tblBenchMethodSelection").on( "click","tr", function(){
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
			$('#btnBack').fadeIn('fast');
			$('#btnPrev').fadeOut('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 1:	// Run space choice stage
			$('#fieldSolverMethod').fadeIn('fast');
			$('#btnNext').fadeOut('fast');
			$('#btnBack').fadeOut('fast');
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
	}
}