// All the classes given to a checkbox 
var orderedCheckboxClasses = ['.cpuTimeCheckbox','.memUsageCheckbox','.wallclockCheckbox'];

// A mapping of each checkbox class to the class it controls the visibility of
var classControlledByCheckbox = {
	'.cpuTimeCheckbox'  : '.cpuTime',
	'.memUsageCheckbox' : '.memUsage',
	'.wallclockCheckbox': '.wallclock'
};

// A mapping from each checkbox to whether or not that checkbox is enabled.
var checkboxEnabled = {
	'.cpuTimeCheckbox': true,
	'.memUsageCheckbox': true,
	'.wallclockCheckbox': true
};


// Entry point to JavaScript application.
$(document).ready(function() {
	registerCheckboxEventHandlers();

	removeHeader();

	$('.jobMatrix').each(function(index, matrix) {;
		var table = $(matrix).dataTable({
			/*
			'columnDefs': [
				{ 'width': '120px', 'targets': '_all' }
			],
			*/
			'bSort': false,
			'scrollY': '300px',
			'scrollX': '100%',
			'scrollCollapse': true,
			'paging': false
		});

		table.fnAdjustColumnSizing();


		new $.fn.dataTable.FixedColumns(table);
	});


	$('#selectStageButton').click(function() {
		console.log('Select stage button clicked.');
		var stageToRedirectTo = $('#selectStageInput').val();
		console.log('Input value is '+stageToRedirectTo);
		if (isInt(stageToRedirectTo)) {
			console.log('Input value is an integer, redirecting.');
			var jobId = $('#jobId').text();
			window.location.replace(starexecRoot + '/secure/details/jobMatrixView.jsp?id='+jobId+'&stage='+stageToRedirectTo);
		} else {
			console.log('Input value is not an integer, showing error message.');
			$('#selectStageError').show();
		}
	});


});

function overrideDataTablesCss() {
	// TODO there's probably a better way to do this that only involves CSS
	// manually override datatable css settings
	// to align benchmark titles column with other columns
	//$('.benchmarkHeader').css('padding-top', '20px');
}

function isInt(value) {
	var intRegex = /^[1-9]{1}[0-9]*$/;

	return intRegex.test(value);
}

/**
 * Shows or hides each divider class depending on whether the appropriate checkboxes are enabled or not
 * @author Albert Giegerich
 */
function updateDividers() {
	if (checkboxEnabled['.cpuTimeCheckbox'] && checkboxEnabled['.memUsageCheckbox']) {
		$('.cpuTimeMemUsageDivider').show();
	} else {
		$('.cpuTimeMemUsageDivider').hide();
	}

	if (checkboxEnabled['.memUsageCheckbox'] && checkboxEnabled['.wallclockCheckbox']) {
		$('.memUsageWallclockDivider').show();
	} else {
		$('.memUsageWallclockDivider').hide();
	}

	if (checkboxEnabled['.cpuTimeCheckbox'] && !checkboxEnabled['.memUsageCheckbox'] && checkboxEnabled['.wallclockCheckbox']) {
		$('.cpuTimeWallclockDivider').show();
	} else {
		$('.cpuTimeWallclockDivider').hide();
	}
}

/**
 * Shows/hides a checkbox as well as updating it's status in checkboxEnabled
 * @param checkboxClass The checkbox class to be toggled.
 * @author Albert Giegerich
 */
function toggleCheckbox(checkboxClass) {
	$(classControlledByCheckbox[checkboxClass]).toggle();
	checkboxEnabled[checkboxClass] = !checkboxEnabled[checkboxClass];
}

/**
 * Registers an on-click event handler for each checkbox class.
 * @author Albert Giegerich
 */
function registerCheckboxEventHandlers() {
	orderedCheckboxClasses.forEach(function(checkboxClass) {
		$(checkboxClass).click(function() {
			// Whenever a checkbox class is clicked toggle it
			// and update all dividers on the page to reflect
			// the new state
			toggleCheckbox(checkboxClass);
			updateDividers();
		});
	});
}

function removeHeader() {
	$('#pageHeader').remove();
}

/**
 * Makes the header stay in the same place despite horizontal scrolling.
 * @author Albert Giegerich
 */
function fixHeaderHorizontally() {
	var leftOffset = parseInt($('#pageHeader').css('left'));
	$(window).scroll(function() {
		$('#pageHeader').css({
			'left': $(this).scrollLeft() + leftOffset 
		});
	});
}


