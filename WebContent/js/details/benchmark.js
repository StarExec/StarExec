var bid;

$(document).ready(function(){
	bid = getParameterByName('id');	
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);
	$('#fieldContents').expandable(true, function() {
		if($(this).data('requested') == undefined) {
			$(this).data('requested', true);
			
			$('#fieldContents legend img').show();
			$.get(starexecRoot+'services/benchmarks/' + bid + '/contents?limit=100', function(data) {
				$('#benchContent').text(data);
				$('#fieldContents legend img').hide();
			}).error(function(){				
				$('#benchContent').text('unavailable');
				$('#fieldContents legend img').hide();
			});
		}
	});
	$('#actions').expandable(true);
	
	
	$('#downLink').unbind("click");
	$('#downLink').click(function() {
		createDialog("Processing your download request, please wait. This will take some time for large benchmarks.");
		token=Math.floor(Math.random()*100000000);
		$('#downLink').attr('href', starexecRoot+"secure/download?token=" +token+ "&type=bench&id="+$("#benchId").attr("value"));
		destroyOnReturn(token);
	});
	
	
});


