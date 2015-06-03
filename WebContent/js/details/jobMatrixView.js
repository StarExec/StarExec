// All the classes given to a checkbox 
var orderedCheckboxClasses = ['.runtimeCheckbox','.memUsageCheckbox','.wallclockCheckbox'];

// A mapping of each checkbox class to the class it controls the visibility of
var classControlledByCheckbox = {
	'.runtimeCheckbox'  : '.runtime',
	'.memUsageCheckbox' : '.memUsage',
	'.wallclockCheckbox': '.wallclock'
};

// A mapping from each checkbox to whether or not that checkbox is enabled.
var checkboxEnabled = {
	'.runtimeCheckbox': true,
	'.memUsageCheckbox': true,
	'.wallclockCheckbox': true
};


// Entry point to JavaScript application.
$(document).ready(function() {
	registerCheckboxEventHandlers();

	var table = $('.jobMatrix').DataTable({
		"columnDefs": [
			{ "width": "120px", "targets": "_all" }
		],
		"scrollY": "300px",
		"scrollX": true,
		"scrollCollapse": true,
		"bSort": false,
		"paging": false
	});

	new $.fn.dataTable.FixedColumns(table, {
		leftColumns: 1,
		rightColumns: 0
	});

	// TODO there's probably a better way to do this that only involves CSS
	// manually override datatable css settings
	// to align benchmark titles column with other columns
	$('.benchmarkHeader').css('padding-top', '20px');

});

/**
 * Shows or hides each divider class depending on whether the appropriate checkboxes are enabled or not
 * @author Albert Giegerich
 */
function updateDividers() {
	if (checkboxEnabled['.runtimeCheckbox'] && checkboxEnabled['.memUsageCheckbox']) {
		$('.runtimeMemUsageDivider').show();
	} else {
		$('.runtimeMemUsageDivider').hide();
	}

	if (checkboxEnabled['.memUsageCheckbox'] && checkboxEnabled['.wallclockCheckbox']) {
		$('.memUsageWallclockDivider').show();
	} else {
		$('.memUsageWallclockDivider').hide();
	}

	if (checkboxEnabled['.runtimeCheckbox'] && !checkboxEnabled['.memUsageCheckbox'] && checkboxEnabled['.wallclockCheckbox']) {
		$('.runtimeWallclockDivider').show();
	} else {
		$('.runtimeWallclockDivider').hide();
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


