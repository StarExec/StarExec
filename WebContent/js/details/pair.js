$(document).ready(function(){
	$('#pairTbl').dataTable( {
        "sDom": 'rt<"bottom"flpi><"clear">'
    });
	
	$('#detailTable').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',
        "aaSorting": [],
        "bPaginate": false,        
        "bSort": true        
    });
	
	$('#pairAttrs').dataTable( {
        "sDom": 'rt<"bottom"f><"clear">',        
        "bPaginate": false
    });
	
	$('#downLink').button({
		icons: {
			secondary: "ui-icon-arrowthick-1-s"
    }});
	
	$('#fieldDetails').expandable(false);
	$('#fieldStats').expandable(true);
	$('#fieldAttrs').expandable(true);
	
	$('#fieldOutput').expandable(true, function() {
		if($(this).data('requested') == undefined) {
			$(this).data('requested', true);
			
			$('#fieldOutput legend img').show();
			var pid = getParameterByName('id');			
			$.get(starexecRoot+'services/jobs/pairs/' + pid + '/stdout?limit=100', function(data) {
				$('#jpStdout').text(data);
				$('#fieldOutput legend img').hide();
			}).error(function(){
				$('#jpStdout').text('unavailable');
				$('#fieldOutput legend img').hide();
			});
		}
	});		
	
	$('#fieldLog').expandable(true, function() {
		if($(this).data('requested') == undefined) {
			$(this).data('requested', true);
			
			$('#fieldLog legend img').show();
			
			var pid = getParameterByName('id');
			$.get(starexecRoot+'services/jobs/pairs/' + pid + '/log', function(data) {
				$('#jpLog').text(data);
				$('#fieldLog legend img').hide();
			}).error(function(){
				$('#jpLog').text('unavailable');
				$('#fieldLog legend img').hide();
			});
		}
	});
	
	// Hide loading images by default
	$('legend img').hide();
});