$(document).ready(function(){
	initDataTables();
	
	$('#trashcan').button({
		icons: {
			secondary: "ui-icon-trash"
		}});
	
	$('#pauseAll').button({
		icon: {
			secondary: "ui-icon-pause"
	}});
	
	$('#trashcan').hide();
	
	$('#jobs tbody tr').live('click', function () {
		   $(this).toggleClass( 'row_selected' );
		} );
	
});

function initDataTables() {
	// Setup the DataTable objects
	jobTable = $('#jobs').dataTable( {
		"sDom"			: 'rt<"bottom"flpi><"clear">',
		"iDisplayStart"	: 0,
		"iDisplayLength": 10,
		"bServerSide"	: true,
		"sAjaxSource"	: starexecRoot+"services/",
		"sServerMethod" : 'POST',
		"fnServerData"	: fnPaginationHandler
	});
}

function fnPaginationHandler(sSource, aoData, fnCallback) {

	// Request the next page of primitives from the server via AJAX
	$.post(  
			sSource + "jobs/pagination",
			aoData,
			function(nextDataTablePage){
				switch(nextDataTablePage){
				case 1:
					showMessage('error', "failed to get the next page of results; please try again", 5000);
					break;
				case 2:		
					// This error is a nuisance and the fieldsets are already hidden on spaces where the user lacks permissions
//					showMessage('error', "you do not have sufficient permissions to view primitives in this space", 5000);
					break;
				default:	// Have to use the default case since this process returns JSON objects to the client

					// Update the number displayed in this DataTable's fieldset
					$('#userExpd').children('span:first-child').text(nextDataTablePage.iTotalRecords);
				
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
				
				// Make the table that was just populated draggable too
				initDraggable('#jobs');
				
				colorizeJobStatistics();

				break;
				}
			},  
			"json"
	).error(function(){
		//showMessage('error',"Internal error populating table",5000); Seems to show up on redirects
	});
}

/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 * @author Tyler Jensen & Todd Elvers
 */
function initDraggable(table) {
	var rows = $('#jobs').children('tbody').children('tr');

	// Using jQuery UI, make the first column in each row draggable
	rows.draggable({
		cursorAt: { cursor: 'move', left: -1, bottom: -1},	// Set the cursor to the move icon and make it start in the corner of the helper		
		containment: 'document',							// Allow the element to be dragged anywhere in the document
		distance: 20,										// Only trigger a drag when the distanced dragged is > 20 pixels
		scroll: true,										// Scroll with the page as the item is dragged if needed
		helper: getDragClone,								// The method that returns the 'cloned' element that is dragged
		start: onDragStart,									// Method called when the dragging begins
		stop: onDragStop									// Method called when the dragging ends
	});
	
	

	// Set the JQuery variables used during the drag/drop process
	$.each(rows, function(i, row){
		$(row).data("id", $(row).children('td:first-child').children('input').val());
		$(row).data("type", $(row).children('td:first-child').children('input').attr('prim'));

		// if it is comment then do not display the first field
		if($(row).data('type') !== undefined && $(row).data('type')[0] == 'c'){
			$(row).data("name","this comment");
		}else{
			$(row).data("name", $(row).children('td:first-child').children('a').text());
		}
	});

	// Make the trash can in the explorer list be a droppable target
	$('#trashcan').droppable({
		drop		: onTrashDrop,
		tolerance	: 'touch',	// Use the pointer to determine drop position instead of the middle of the drag clone element
		hoverClass	: 'hover',		// Class applied to the space element when something is being dragged over it
		activeClass	: 'active'		// Class applied to the space element when something is being dragged
	});
	

	log($(table).attr('id') + ' table initialized as draggable');
}

/**
 * Returns the html of an element that is dragged along with the mouse when an item is dragged on the page
 * @author Tyler Jensen
 */
function getDragClone(event) {	
	var src = $(event.currentTarget);
	if(false == $(src).hasClass('row_selected')){ //change
		$(src).addClass('row_selected');
	}
	var ids = getSelectedRows($(src).parents('table:first'));

	var txtDisplay = $(src).children(':first-child').text();
	var icon = 'ui-icon ';	
	var primType = $(src).data('type');
	log(src);


	if(ids.length > 1) {
		txtDisplay = ids.length + ' ' + primType + 's';
	}

	// Change the drag icon based on what the type of object being dragged is
	switch(primType[0]){
	case 'u':
		icon += 'ui-icon-person';
		break;
	default:
		icon += 'ui-icon-newwin';
	break;
	}

	// Return a styled div with the name of the element that was originally dragged
	return '<div class="dragClone"><span class="' + icon + '"></span>' + txtDisplay + '</div>';
	alert("found drag clone");
}

/**
 * Called when any item is starting to be dragged within the browser
 */
function onDragStart(event, ui) {
	log('drag started');
	$('#trashcan').show();
}

/**
 * Called when there is no longer anything being dragged
 */
function onDragStop(event, ui) {
	log('drag stopped');
	$('#trashcan').hide();
}


/**
 * Called when a draggable item (primitive) is dropped on the trash can
 * @author Todd Elvers
 */
function onTrashDrop(event, ui){
	// Collect the selected elements from the table being dragged from
	var ids = getSelectedRows($(ui.draggable).parents('table:first'));

	if(ids.length < 2) {
		// If 0 or 1 things are selected in the table, just use the element that is being dragged
		ids = [ui.draggable.data('id')];
	}

	// Call the appropriate primitive removal function
	switch(ui.draggable.data('type')[0]){
	case 'j':
		alert("job paused");
		break;
	}
}

/**
 * For a given dataTable, this extracts the id's of the rows that have been
 * selected by the user
 * 
 * @param dataTable the particular dataTable to extract the id's from
 * @returns {Array} list of id values for the selected rows
 * @author Todd Elvers
 */
function getSelectedRows(dataTable){
	var idArray = new Array();
	var rows = $(dataTable).children('tbody').children('tr.row_selected');
	$.each(rows, function(i, row) {
		idArray.push($(this).children('td:first').children('input').val());
	});
	return idArray;
}

function editPermissions(userId) {
	window.location.replace("permissions.jsp?id=" + userId);
}

/**
 * Colorize the job statistics in the jobTable
 */
function colorizeJobStatistics(){
	// Colorize the statistics in the job table for completed pairs
	$("#jobs p.asc").heatcolor(
			function() {
				// Return the floating point value of the stat
				var value = $(this).text();
				return eval(value.slice(0, -1));				
			},
			{ 
				maxval: 100,
				minval: 0,
				colorStyle: 'greentored',
				lightness: 0 
			}
	);
	//colorize the unchanging totals
	$("#jobs p.static").heatcolor(
			function() {
				// Return the floating point value of the stat
				return eval(1);				
			},
			{ 
				maxval: 1,
				minval: 0,
				colorStyle: 'greentored',
				lightness: 0 
			}
	);
	// Colorize the statistics in the job table (for pending and error which use reverse color schemes)
	$("#jobs p.desc").heatcolor(
			function() {
				var value = $(this).text();
				return eval(value.slice(0, -1));	
			},
			{ 
				maxval: 100,
				minval: 0,
				colorStyle: 'greentored',
				reverseOrder: true,
				lightness: 0 
			}
	);



}
