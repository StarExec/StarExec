/**
 * Initializes a table so that elements can be dragged out of it and onto a space name
 * @param table The table to make draggable
 * @author Tyler Jensen & Todd Elvers
 */
function makeTableDraggable(table, dragStart, dragClone) {
	var rows = $(table).children('tbody').children('tr');
	// Using jQuery UI, make the first column in each row draggable
	rows.draggable({
		cursorAt: { cursor: 'move', left: -1, bottom: -1},	// Set the cursor to the move icon and make it start in the corner of the helper		
		//containment: 'document',							// Allow the element to be dragged anywhere in the document
		distance: 20,										// Only trigger a drag when the distanced dragged is > 20 pixels
		scroll: true,										// Scroll with the page as the item is dragged if needed
		helper: dragClone,								// The method that returns the 'cloned' element that is dragged
		start: dragStart									// Method called when the dragging begins
	});
	// Set the JQuery variables used during the drag/drop process
	$.each(rows, function(i, row){
		$(row).data("id", $(row).children('td:first-child').children('input').val());
		$(row).data("type", $(row).children('td:first-child').children('input').attr('prim'));

		
		$(row).data("name", $(row).children('td:first-child').children('a').text());
		
	});
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
	case 'b':
		icon += 'ui-icon-script';
		break;
	case 'j':
		icon += 'ui-icon-gear';
		break;
	
	default:
		icon += 'ui-icon-newwin';
	break;
	}
	// Return a styled div with the name of the element that was originally dragged
	return '<div class="dragClone"><span class="' + icon + '"></span>' + txtDisplay + '</div>';
}