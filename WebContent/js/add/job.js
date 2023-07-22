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
solverUndo = [];
var solverUndo = [];
var benchUndo = [];

var benchMethodVal = 0;  //1 if choose from space, 2 if choose from hier, 0 otherwise
var benchTable = null;
var curSpaceId = null;
var isLeaf = null;
var hasNoBenchmarks = false;	
$(document).ready(function() {
	//see if we are in readOnly mode and redirect appropriatly	
	$.get(starexecRoot + "services/isReadOnly", function(response) {
		if (response == "true") {
			window.location.href = starexecRoot + "/secure/add/addJobLocked.jsp";
		}
	  }).fail(function(xhr, status, error) {
		console.log("Error: " + error);
	  });
	curSpaceId = $("#spaceIdInput").attr("value");
	log('curSpaceId=' + curSpaceId);
	checkIfLeafSpace(curSpaceId);

	initUI();
	fetch(starexecRoot + "services/job/" + curSpaceId + "/allbench/pagination", {
		method: 'POST',
	  })
		.then(response => response.json())
		.then(data => {
		  // Handle the response data
			var aaData = data.aaData;
			if (aaData.length == 0) {
				hasNoBenchmarks = true;
			}
		})
		.catch(error => {

		  console.error('Error:', error);
		});
	attachFormValidation();

	$('#radioDepth').prop('checked', true);
	$('#radioNoPause').prop('checked', true);

	// Remove all unselected rows from the DOM before submitting
	$('#addForm').submit(function() {
		$('#tblBenchConfig tbody')
		.children('tr')
		.not('.row_selected')
		.find('input')
		.remove();
		//$('#tblBenchHier tbody').children('tr').not('.row_selected').find('input').remove();
		$('#tblSpaceSelection tbody')
		.children('tr')
		.not('.row_selected')
		.find('input')
		.remove();
		$('#tblSolverConfig tbody')
		.children('tr')
		.not('.row_selected')
		.find('input')
		.remove();
		$('#tblBenchMethodSelection tbody')
		.children('tr')
		.not('.row_selected')
		.find('input')
		.remove();
		return true;
	});
	if ($("#remainingQuota").attr("value") <= 0) {
		showMessage("warn",
			"You have reached your pair quota. You will not be able to submit new jobs without removing some old jobs first.",
			8000);
	}
});

function getMaxCpuTimeout() {
	maxtime = $("#workerQueue option:selected").attr("cpumax");
	return parseInt(maxtime);
}

function getMaxWallTimeout() {
	maxtime = $("#workerQueue option:selected").attr("wallmax");
	return parseInt(maxtime);
}

function getCpuTimeoutErrorMessage() {
	timeout = getMaxCpuTimeout();
	if (isNaN(timeout)) {
		return "please select a queue";
	}
	return timeout + " second max timeout";
}

function getClockTimeoutErrorMessage() {
	timeout = getMaxWallTimeout();
	if (isNaN(timeout)) {
		return "please select a queue";
	}
	return timeout + " second max timeout";
}

/**
 * Attach validation to the job creation form
 */
function attachFormValidation() {
	// Add regular expression capabilities to the validator
	addValidators();

	// Set up form validation
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: $("#txtJobName").attr("length"),
				jspregex: "DUMMY REGEX"
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
				min: 0
			},
			queue: {
				required: true
			},
			resultsInterval: {
				required: true,
				interval: "10 min"
			}
		},
		messages: {
			name: {
				required: "enter a job name",
				minlength: "2 characters minimum",
				maxlength: $("#txtJobName")
				.attr("length") + " characters maximum",
				jspregex: "invalid character(s)"
			},
			desc: {
				required: "enter a job description",
				maxlength: $("#txtDesc").attr("length") + " characters maximum",
				regex: "invalid character(s)"
			},
			cpuTimeout: {
				required: "enter a timeout",
				max: getCpuTimeoutErrorMessage(),
				min: "1 second minimum timeout"
			},
			wallclockTimeout: {
				required: "enter a timeout",
				max: getClockTimeoutErrorMessage(),
				min: "1 second minimum timeout"
			},
			maxMem: {
				required: "enter a maximum memory",
				max: "100 gigabytes maximum"
			},
			queue: {
				required: "error - no worker queues"
			},
			resultsInterval: {
				required: "enter a results interval",
				interval: "must be at least 10 seconds"
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
			max: getMaxCpuTimeout() + " second max timeout",
			min: "1 second minimum timeout"
		};

		settings.messages.wallclockTimeout = {
			required: "enter a timeout",
			max: getMaxWallTimeout() + " second max timeout",
			min: "1 second minimum timeout"
		};
		$("#addForm").valid(); //revalidate now that we have new rules

	});
}

/**
 * Sets up the jQuery button style and attaches click handlers to those buttons.
 */
function initUI() {

	var advancedJobOptionsCollapsed = $.cookie('advancedJobOptions') != 'false';
	$('#advancedOptions')
	.expandable(advancedJobOptionsCollapsed)
	.children('legend:first')
	.click(function() {
		var advancedJobOptionsCollapsed = !$(this).data('open');
		$.cookie('advancedJobOptions',
			advancedJobOptionsCollapsed,
			{expires: 10000, path: '/'});
	});

	//If there is only one post processor and for some reason it is not the default, set it as such
	if ($("#postProcess").find("option").length == 2) {
		$("#postProcess").find("option").last().prop("selected", true);
	}
	//If there is only one pre processor and for some reason it is not the default, set it as such
	if ($("#preProcess").find("option").length == 2) {
		$("#preProcess").find("option").last().prop("selected", true);
	}

	$("#tblBenchConfig").dataTable({
		"sDom": 'rt<"bottom"f><"clear">',
		"bPaginate": false,
		"bSort": true,
		"sAjaxSource": starexecRoot + "services/job/" + curSpaceId + "/allbench/pagination",
		"sServerMethod": "POST",
		"fnServerData": fnBenchPaginationHandler
	});
	// Set up datatables
	setupSolverConfigDataTable();

	// Place the select all/none buttons in the datatable footer
	/*$('#fieldStep3 div.solverSelectWrap').detach().prependTo('#fieldStep3 div.bottom');
	$('#fieldStep4 div.solverSelectWrap').detach().prependTo('#fieldStep4 div.bottom');*/
	$('#fieldSelectBenchSpace div.solverSelectWrap')
	.detach()
	.prependTo('#fieldSelectBenchSpace div.bottom');
	$('#fieldSolverSelection div.solverSelectWrap')
	.detach()
	.prependTo('#fieldSolverSelection div.bottom');

	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function() {

		history.back(-1);
	});

	/**
	 * When a user clicks the "Advanced Options" button, we want to display all
	 * the advanced options *except* those that are not relevant for the current
	 * benchmarking framework. Therefore, we exclude these options from our
	 * toggle set and handle them elsewhere.
	 */
	$('.advancedOptions').hide();
	$('#advancedOptionsButton').button({
		icons: {
			primary: "ui-icon-gear"
		}
	}).click(function() {
		$('#advancedOptionsRow').hide();
		$('.advancedOptions:not(.runsolveronly,.benchexeconly)').show();
		benchmarkingFrameworkChanged();
	});

	$('#btnNext').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-e"
		}
	}).click(function() {
		if (progress === 0 || progress === 3) {
			nextState();
		}
	});

	$('#btnPrev').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function() {
		switch (progress) {
			case 1:
				progress -= 1;
				break;
			case 2:
				clearSelectedRowsOnTable($('#tblSpaceSelection'));
				progress -= 1;
				break;
			case 3:
				clearSelectedRowsOnTable($('#tblBenchMethodSelection'));
				progress -= 1;
				break;
			case 4:
				progress -= 1;
				break;
			case 5:
				//must go back two if choosing from hierarchy
				progress -= 2;
				break;
		}
		updateProgress();
	});

	$('#btnDone').button({
		icons: {
			secondary: "ui-icon-check"
		}
	}).click(function() {
		// Make sure the user has at least one benchmark in the table
		if (progress == 4) {
			if (hasNoBenchmarks) {
				showMessage('warn',
				'Your Space has no benchmarks, please upload a benchmark before creating a job',
				3000);
			return false;
			}
			if ($('#tblBenchConfig tbody tr.row_selected').length <= 0) {
				showMessage('warn',
				'You must have at least one benchmark for this job.',
				3000);
			return false;
			}
			
			
		}
		if (progress == 3 && $('#tblSolverConfig tbody tr.row_selected').length <= 0) {
			// Make sure the user selects at least one solver before moving on
			showMessage('warn',
				'You must have at least one solver for this job.',
				3000);
			return false;
		}
		createDialog(
			"Creating your job, please wait. This will take some time for large jobs.");
	});

	// Hook up select all/none buttons for solvers
	registerSelectAllSolversEventHandler();
	registerSelectNoneSolversEventHandler();
	registerSelectAllConfigsEventHandler();
	registerSelectNoneConfigsEventHandler();

	$('.selectAllBenchmarks').click(function() {
		$('#tblBenchConfig tbody tr').addClass('row_selected');
	});
	$('.selectNoneBenchmarks').click(function() {
		$('#tblBenchConfig tbody tr').removeClass('row_selected');
	});

	log('Setting up runsolver/BenchExec clicked functions.');
	$('#editBenchmarkingFramework').change(benchmarkingFrameworkChanged);

	registerSolverConfigTableRowSelectionEventHandler();

	$("#tblBenchConfig").on("click", "tr", function() {
		$(this).toggleClass("row_selected");
	});

	// Step 2 related actions
	// Selection toggling

	// quick hierarchy run selected
	//$("#runSpace, #runHierarchy, #keepHierarchy").click(function() {
	$("#keepHierarchy").click(function() {
		$("#tblBenchConfig tr").addClass("row_selected");
		$("#tblSolverConfig tr").addClass("row_selected");
		$("#tblSolverConfig tr").find('input').prop('checked', true);
		$('#btnNext').fadeOut('fast');
		$('#btnDone').fadeIn('fast');
		addRowSelectedAndClearSiblings(this);
	});

	// Choose benchmarks selected
	$("#runChoose").click(function() {
		$("#tblBenchConfig tr").removeClass("row_selected");
		$("#tblSolverConfig tr").removeClass("row_selected");
		$("#tblSolverConfig tr").find('input').prop('checked', false);
		$('#btnDone').fadeOut('fast');
		/*
		$('#btnNext').fadeIn('fast');
		$('#btnNext').fadeIn('fast');
		*/
		addRowSelectedAndClearSiblings(this);
		nextState();
	});

	// Choose benchmarks in space selected
	$("#someBenchInSpace").click(function() {
		benchSelectionClick(1, this);
	});

	// all benchmarks in space
	$("#allBenchInSpace").click(function() {
		benchSelectionClick(0, this);
	});

	// all benchmarks in hierarchy
	$("#allBenchInHierarchy").click(function() {
		benchSelectionClick(0, this);
	});

	// Set timeout default to 1 day
	$("#timeoutDay option[value='1']").attr("selected", "selected");

	// Initialize the state of the job creator by forcing a progress update
	updateProgress();

}

function benchmarkingFrameworkChanged() {
	'use strict';

	var newBenchmarkingFramework = $('#editBenchmarkingFramework')
	.find(':selected')
	.attr('value');
	if (newBenchmarkingFramework === 'BENCHEXEC') {
		$('.benchexeconly').show();
		$('.runsolveronly').hide();
	} else {
		$('.benchexeconly').hide();
		$('.runsolveronly').show();
	}
}

function benchSelectionClick(benchMethod, row) {
	benchMethodVal = benchMethod;
	$("#tblBenchConfig tr").removeClass("row_selected");
	$("#tblSolverConfig tr").removeClass("row_selected");
	$("#tblSolverConfig tr").find('input').prop('checked', false);
	addRowSelectedAndClearSiblings(row);
	nextState();
}

function nextState() {
	var isValid = $('#addForm').valid();

	// Make sure the job config form is valid  before moving on
	if (progress == 0 && false == isValid) {
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
		showMessage('warn',
			'You must have at least one solver for this job.',
			3000);
		return;
	}

	// Move on to the next step if everything is valid
	if (progress != 3) {
		progress++;
	}
	else {
		//if choosing bench from space
		if (benchMethodVal == 1) {
			progress = 4;
		}
	}
	updateProgress();
}

function addRowSelectedAndClearSiblings(row) {
	$(row).addClass('row_selected');
	$(row).siblings().removeClass("row_selected");
}

function checkIfLeafSpace(spaceId) {
	var isLeafSpace = null;
	$.get(
		starexecRoot + 'services/space/isLeaf/' + spaceId,
		{},
		function(data) {
			isLeafSpace = data;
			log('isLeafSpace=' + isLeafSpace);
		},
		'json'
	).done(function() {
		if (isLeafSpace) {
			log('Determined that current space is a leaf space.');
			hideHierarchyRelatedFunctionality();
		}
	}).fail(function() {
		log('retrieving isLeaf failed');
	});
}

function hideHierarchyRelatedFunctionality() {
	log('Hiding hierarchy related functionality.');
	$('#allBenchInHierarchy').hide();

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

	log("Progress: " + progress);
	switch (progress) {
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
			clearSelectedRowsOnTable($('#tblSpaceSelection'));
			break;
		case 2:	// If quick run space method not chosen, how to select benchmarks
			$('#fieldBenchMethod').fadeIn('fast');
			$('#btnPrev').fadeIn('fast');
			$('#btnDone').fadeOut('fast');
			break;
		case 3:	// selecting solvers and configurations
			$('#fieldSolverSelection').fadeIn('fast');
			$('#btnPrev').fadeIn('fast');
			//still need to select benchmarks
			if (benchMethodVal > 0) {
				$('#btnNext').fadeIn('fast');
				$('#btnDone').fadeOut('fast');
			}
			//benchmarks already selected
			else {
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

function clearSelectedRowsOnTable(table) {
	$(table).find('tr').each(function(index, element) {
		$(element).removeClass('row_selected');
	});
}

function fnBenchPaginationHandler(sSource, aoData, fnCallback) {
	$.post(
		sSource,
		aoData,
		function(nextDataTablePage) {

			s = parseReturnCode(nextDataTablePage);
			if (s) {
				fnCallback(nextDataTablePage);
			}

		},
		"json"
	).error(function() {
		showMessage('error', "Internal error populating data table", 5000);
	});
}
