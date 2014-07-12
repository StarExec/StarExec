//sort buttons must have the sortButton class

var sortOverride=null;
var sortASC="true";
var theSortTable=null;
function setSortTable(tbl) {
	theSortTable=tbl;
}

function attachSortButtonFunctions(tbl){
	theSortTabe=tbl;
	$(".sortButton").button({
		icons: {
			primary: "ui-icon-arrowthick-1-n"
		}
	});
	
	$(".sortButton").click(function(){
		if (sortOverride == $(this).attr("value")) {
			if ($(this).attr("asc")=="true") {
				$(this).attr("asc","false");
				$(this).button("option", {
			          icons: { primary: "ui-icon-arrowthick-1-s" }
			    });
				sortASC="true";

			} else {
				$(this).attr("asc","true");
				$(this).button("option", {
			          icons: { primary: "ui-icon-arrowthick-1-n" }
			    });
				sortASC="false";

			}
			
		} else {
			resetSortButtons();
			
			$(this).attr("asc","false");
			$(this).button("option", {
		          icons: { primary: "ui-icon-arrowthick-1-s" }
		    });
			
			sortOverride=$(this).attr("value");
		}
		theSortTable.fnDraw(false);

	});
	
}

function resetSortButtons() {
	$(".sortButton").button("option", {
        icons: { primary: "ui-icon-arrowthick-1-n" }
  });
	$(".sortButton").attr("asc","true");
	sortOverride=null; //now we sort by a column
	sortASC="true";
}

function getSelectedSort() {
	return sortOverride;
}

function isASC() {
	return sortASC;
}