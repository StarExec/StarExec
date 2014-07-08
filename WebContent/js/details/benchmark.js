var bid;

$(document).ready(function(){
	bid = getParameterByName('id');	
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);
	$( "#dialog-warning").hide();
	
	$('#fieldContents').expandable(true);
	$('#actions').expandable(true);

	
	$('#downLink').unbind("click");
	$('#downLink').click(function() {
		createDialog("Processing your download request, please wait. This will take some time for large benchmarks.");
		token=Math.floor(Math.random()*100000000);
		$('#downLink').attr('href', starexecRoot+"secure/download?token=" +token+ "&type=bench&id="+$("#benchId").attr("value"));
		destroyOnReturn(token);
	});
	
	
});


