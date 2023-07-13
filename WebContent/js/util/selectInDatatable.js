//given a table, select all rows that are between the two selected rows
function selectAllBetween(table) {
	row = $(table).find("tr.row_selected:first");
	//if we could actually find the row
	if (row.size() > 0) {
		row = row.next("tr");
		while (!row.hasClass("row_selected")) {
			row.addClass("row_selected");
			row = row.next("tr");
			//if we couldn't get a next row
			if (row.size() == 0) {
				break;
			}
		}
	}
}

/* 
* given a datatable, select all the items in that table. 
* NOTE: only selects all in the page that you are currently
* on, changing pages requires a new api call and page refresh.
* @author aguo2
*/
function selectAllInTable(table) {
	var row = $(table).find("tr");
	var no1stele = row.slice(1);
	while (no1stele.size() != 0) {
		//WARNING, IF YOU DON'T SPLICE HERE, JQUERY WILL SELECT ALL THE ROWS!!
		//I don't understand how it worked in the other function. What matters
		//is that it works now
		var currentEl = no1stele.slice(0,1);
		if (!currentEl.hasClass("row_selected")) {
			currentEl.addClass("row_selected");
		}
		no1stele = no1stele.next("tr");

	}
	}

/* 
* given a datatable, unselect all the items in that table. 
* NOTE: only unselects all in the page that you are currently
* on, changing pages requires a new api call and page refresh.
* @author aguo2
*/
function selectNoneInTable(table) {
	var row = $(table).find("tr");
	var no1stele = row.slice(1);
	while (no1stele.size() != 0) {
		//WARNING, IF YOU DON'T SPLICE HERE, JQUERY WILL SELECT ALL THE ROWS!!
		//I don't understand how it worked in the other function. What matters
		//is that it works now
		var currentEl = no1stele.slice(0,1);
		if (currentEl.hasClass("row_selected")) {
			currentEl.removeClass("row_selected");
		}
		no1stele = no1stele.next("tr");

	}
	}


function selectFirstN(table, n) {

	var row = $(table).find("tr");
	var no1stele = row.slice(1);
	while (n > 0) {
		//if there are no more rows
		if (no1stele.size() == 0) {
			break;
		}
		//WARNING, IF YOU DON'T SPLICE HERE, JQUERY WILL SELECT ALL THE ROWS!!
		//I don't understand how it worked in the other function. What matters
		//is that it works now
		var currentEl = no1stele.slice(0,1);
		if (!currentEl.hasClass("row_selected")) {
			currentEl.addClass("row_selected");
		}
		no1stele = no1stele.next("tr");
		n--;

	}
}