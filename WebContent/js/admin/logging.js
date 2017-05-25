var levelTable;

$(document).ready(function(){
	initUI();

});



function initUI(){

	$("#applyAll").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#applyToClass").button({
		icons: {
			primary: "ui-icon-check"
		}
	});
	$("#applyToClassAllOthersOff").button({
		icons: {
			primary: "ui-icon-check"
		}
	});


	$("#applyAll").click(function() {
		value=getSelectedRowValue(levelTable);
		$.post(
				starexecRoot+"services/logging/"+value,
				{},
				function(returnCode) {
					parseReturnCode(returnCode);

				},
				"json"
		);
	});

	$("#applyToClass").click(function() {
		value=getSelectedRowValue(levelTable);
		$.post(
			starexecRoot+"services/logging/"+value+"/"+$("#className").val(),
			{},
			function(returnCode) {
				parseReturnCode(returnCode);

			},
			"json"
		);
	});
	$("#applyToClassAllOthersOff").click(function() {
		value=getSelectedRowValue(levelTable);
		$.post(
			starexecRoot+"services/logging/allOffExcept/"+value+"/"+$("#className").val(),
			{},
			function(returnCode) {
				parseReturnCode(returnCode);

			},
			"json"
		);
	});


	levelTable=$('#tableLevels').dataTable( {
        "sDom"			: getDataTablesDom(),
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
        "bSort": false,
        "bPaginate": true
    });


	$("#tableLevels").on( "click", "tr", function() {
		if (!$(this).hasClass("row_selected")) {
			unselectAllRows(levelTable);
		}
		$(this).toggleClass("row_selected");
	});

}

function getSelectedRowValue() {
	log($('.row_selected').attr('value'));
	return $('.row_selected').attr('value');
}

function unselectAllRows(dataTable) {
	$(dataTable).find("tr").each(function() {
		$(this).removeClass("row_selected");
	});
}


