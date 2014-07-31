/**
 * This file contains a function that will select all the rows in a table between two rows that are already selected 
 */

//given a table, select all rows that are between the two selected rows
function selectAllBetween(table) {
	row=$(table).find("tr.row_selected:first");
	//if we could actually find the row
	if (row.size()>0) {
		row=row.next("tr");
		while (!row.hasClass("row_selected")) {
			row.addClass("row_selected");
			row=row.next("tr");
			//if we couldn't get a next row
			if (row.size()==0) {
				break;
			}
		}
	}
}