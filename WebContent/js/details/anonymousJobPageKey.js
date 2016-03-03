'use strict';

$(document).ready(function(){
	$('#solverNameKeyTable').dataTable({
        "sDom"			: 'rt<"bottom"flpi><"clear">',
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
		"pagingType"    : "full_numbers"
	});
});
