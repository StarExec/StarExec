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

var jobId; 
var jobSpaceId; 
var stageNumber; 

// Entry point to JavaScript application.
$(document).ready(function() {
	'use strict';

	jobId = $('#jobId').text();
	jobSpaceId = $('#jobSpaceId').text();
	stageNumber = $('#stageNumber').text();

	registerCheckboxEventHandlers();
	removeHeader();


	var table = $('#jobMatrix').dataTable({
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

		/*table.fnAdjustColumnSizing();*/


	new $.fn.dataTable.FixedColumns(table);


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

	getFinishedJobPairsFromServer();
});

function getFinishedJobPairsFromServer(done) {
	'use strict';
	if (done) {
		return;
	} else {
		$.get(
			starexecRoot+'services/matrix/finished/'+jobId+'/'+jobSpaceId+'/'+stageNumber,
			'',
			function(data) {
				log(data);
				setTimeout(function() {
					updateMatrix(data.benchSolverConfigElementMap);
					getFinishedJobPairsFromServer(data.done);
				}, 5000);
			},
			'json'
		 );
	}
}

function updateMatrix(jobPairData) {
	'use strict';
	for (var key in jobPairData) {
		if (jobPairData.hasOwnProperty(key)) {
			var selector = '#'+key;
			/*log('Number of elements selected with selector='+selector+': '+$(selector).length);*/
			$(selector+' '+'.wallclock').text(jobPairData[key].wallclock);
			$(selector+' '+'.memUsage').text(jobPairData[key].memUsage);
			$(selector+' '+'.cpuTime').text(jobPairData[key].cpuTime);
			$(selector).removeClass('incomplete');
			$(selector).addClass(jobPairData[key].status);
		}
	}
}

function isInt(value) {
	'use strict';
	var intRegex = /^[1-9]{1}[0-9]*$/;

	return intRegex.test(value);
}

/**
 * Shows or hides each divider class depending on whether the appropriate checkboxes are enabled or not
 * @author Albert Giegerich
 */
function updateDividers() {
	'use strict';
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
	'use strict';
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
	'use strict';
	$('#pageHeader').remove();
}

/**
 * Makes the header stay in the same place despite horizontal scrolling.
 * @author Albert Giegerich
 */
function fixHeaderHorizontally() {
	'use strict';
	var leftOffset = parseInt($('#pageHeader').css('left'));
	$(window).scroll(function() {
		$('#pageHeader').css({
			'left': $(this).scrollLeft() + leftOffset 
		});
	});
}


