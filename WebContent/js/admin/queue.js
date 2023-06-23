//This is for Approving/Declining a queue request

var progress = 0;

$(document).ready(function() {
	InitUI();

	$('#btnDone').button({
		icons: {
			secondary: "ui-icon-circle-check"
		}
	});

	initDataTables();

	document.getElementById('qName').innerHTML = document.getElementById(
		"queueName").value;

	var start_date = document.getElementById("start").value;
	dateComponents = start_date.split("/");
	start_date = new Date(dateComponents[2], dateComponents[0] - 1,
		dateComponents[1]);
	var today = new Date();
	today.setHours(0, 0, 0, 0);
	if (start_date < today) {
		$('#dialog-warning-txt')
		.text(
			'WARNING: This request has expired. Please adjust dates accordingly.');

		$('#dialog-warning').dialog({
			modal: true,
			width: 380,
			height: 165,
			buttons: {
				'OK': function() {
					$('#dialog-warning').dialog('close');
				}
			}
		});
		$('#btnDone').hide();
	}

});

function InitUI() {

	$('#btnDecline').button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	}).click(function() {
		$.post(
			starexecRoot + "services/cancel/request/" + getParameterByName("id"),
			function(returnCode) {
				s = parseReturnCode(returnCode);
				if (s) {
					history.back(-1);
				}

			},
			"json"
		);
	});

	$('#btnUpdate').button({
		icons: {
			secondary: "ui-icon-refresh"
		}
	}).click(function() {
		var code = window.location.search.split('code=')[1]; //Check to see if flag was set
		var queueName = document.getElementById("queueName").value;
		var nodeCount = document.getElementById("nodeCount").value;
		var start_date = document.getElementById("start").value;
		var string_start_date = start_date.replace(/\//g, "");
		var end_date = document.getElementById("end").value;
		var string_end_date = end_date.replace(/\//g, "");

		$.post(
			starexecRoot + "services/edit/request/" + code + "/" + queueName + "/" + nodeCount + "/" + string_start_date + "/" + string_end_date,
			function(returnCode) {
				s = parseReturnCode(returnCode);
				if (s) {
					nodeTable.fnDraw();
					document.getElementById('qName').innerHTML = queueName;
				}

			},
			"json"
		).error(function() {
			showMessage('error',
				"Internal error updating user information",
				5000);
		});
	});

	$('#btnBack').button({
		icons: {
			primary: "ui-icon-arrowthick-1-w"
		}
	}).click(function() {

		history.back(-1);
	});

}

function initDataTables() {

	// Setup the DataTable objects
	nodeTable = $('#nodes').dataTable({
		"sDom": getDataTablesDom(),
		"bFilter": false,
		"bInfo": false,
		"bPaginate": false,
		//"bAutoWidth"	: false,
		"iDisplayStart": 0,
		"iDisplayLength": defaultPageSize,
		"bServerSide": true,
		"sAjaxSource": starexecRoot + "services/",
		"sServerMethod": 'POST',
		"fnDrawCallback": function(oSettings) {
			var conflictNumber = 0;

			for (var i = 0, iLen = oSettings.aoData.length; i < iLen; i++) {
				var columnNumber = nodeTable.fnGetData(0).length;
				var conflict = oSettings.aoData[i]._aData[columnNumber - 1];
				var colorCSS = 'statusNeutral';
				if (conflict === 'clear') {
					colorCSS = 'statusClear';
				} else if (conflict === 'CONFLICT') {
					colorCSS = 'statusConflict';
					conflictNumber = conflictNumber + 1;
				} else if (conflict === 'ZERO') {
					colorCSS = 'statusZero';
					conflictNumber = conflictNumber + 1;
				}
				oSettings.aoData[i].nTr.className += " " + colorCSS;
			}
			if (conflictNumber > 0) {
				$('#btnDone').hide();
			} else {
				$('#btnDone').show();
			}
		},
		"fnServerData": fnPaginationHandler
	});
	nodeTable.makeEditable({
		"sUpdateURL": starexecRoot + "secure/update/nodeCount",
		"fnStartProcessingMode": function() {
			nodeTable.fnDraw();

			setTimeout(function() {nodeTable.fnDraw();}, 1000);
		}
	});

}

function fnPaginationHandler(sSource, aoData, fnCallback) {
	var id = window.location.search.split('id=')[1]; //Check to see if flag was set
	// Request the next page of primitives from the server via AJAX
	$.post(
		sSource + "nodes/dates/reservation/" + id + "/pagination",
		aoData,
		function(nextDataTablePage) {
			var s = parseReturnCode(nextDataTablePage);
			if (s) {
				// Replace the current page with the newly received page
				fnCallback(nextDataTablePage);
			}
		},
		"json"
	);
}

