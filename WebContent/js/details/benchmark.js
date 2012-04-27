$(document).ready(function(){
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);
	
	$('#fieldContents').expandable(true, function() {
		if($(this).data('requested') == undefined) {
			$(this).data('requested', true);
			
			$('#fieldContents legend img').show();
			var bid = getParameterByName('id');
			$.get('/starexec/services/benchmarks/' + bid + '/contents?limit=100', function(data) {
				$('#benchContent').text(data);
				$('#fieldContents legend img').hide();
			}).error(function(){				
				$('#benchContent').text('unavailable');
				$('#fieldContents legend img').hide();
			});
		}
	});
			
	// Hide loading images by default
	$('legend img').hide();	
});