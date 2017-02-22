$(document).ready(function(){
	$('#solverNameKeyTable').dataTable({
        "sDom"			: getDataTablesDom(),
        "iDisplayStart"	: 0,
        "iDisplayLength": defaultPageSize,
		"pagingType"    : "full_numbers"
	});
});
