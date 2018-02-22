"use strict";

jQuery(function($) {
	var $levelTable = $('#tableLevels');
	$levelTable.dataTable(new window.star.DataTableConfig({
		"bSort": false,
		"bPaginate": true
	}));

	var getSelectedRowValue = function() {
		return $('.row_selected').attr('value');
	};

	$("#applyAll")
	.button({
		icons: {
			primary: "ui-icon-check"
		}
	})
	.click(function() {
		var value = getSelectedRowValue();
		$.post(
			starexecRoot + "services/logging/" + value,
			{},
			parseReturnCode,
			"json"
		);
	})
	;

	$("#applyToClass")
	.button({
		icons: {
			primary: "ui-icon-check"
		}
	})
	.click(function() {
		var value = getSelectedRowValue();
		var className = $("#className").val();
		$.post(
			starexecRoot + "services/logging/" + value + "/" + className,
			{},
			parseReturnCode,
			"json"
		);
	})
	;

	$("#applyToClassAllOthersOff")
	.button({
		icons: {
			primary: "ui-icon-check"
		}
	})
	.click(function() {
		var value = getSelectedRowValue();
		var className = $("#className").val();
		$.post(
			starexecRoot + "services/logging/allOffExcept/" + value + "/" + className,
			{},
			parseReturnCode,
			"json"
		);
	})
	;

	$("#tableLevels").on("click", "tr", function() {
		if (!$(this).hasClass("row_selected")) {
			$levelTable.find("tr").each(function() {
				$(this).removeClass("row_selected");
			});
		}
		$(this).toggleClass("row_selected");
	});

});
