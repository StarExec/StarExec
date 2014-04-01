var bid;

$(document).ready(function(){
	bid = getParameterByName('id');	
	$('#fieldType').expandable(true);
	$('#fieldAttributes').expandable(true);
	$('#fieldDepends').expandable(true);
	$( "#dialog-warning").hide();
	
	$('#fieldContents').expandable(true);
	$('#actions').expandable(true);
	
	$('#clearCache').button( {
		icons: {
			secondary: "ui-icon-arrowrefresh-1-e"
		}
	});
	
	$("#clearCache").click(function(){
			
			$("#dialog-warning-txt").text('Are you sure you want to clear the cache for this primitive?');		
			$("#dialog-warning").dialog({
				modal: true,
				width: 380,
				height: 165,
				buttons: {
					'clear cache': function() {
						$(this).dialog("close");
						type=$("#cacheType").attr("value");
						$.post(
								starexecRoot+"services/cache/clear/"+bid+"/"+type,
								function(returnCode) {
									if (returnCode<0) {
										showMessage('error',"There was an error clearing the cache for this item",5000);
									}	
						});		
					},
					"cancel": function() {
						$(this).dialog("close");
					}
				}
			});
	});
	
	$('#downLink').unbind("click");
	$('#downLink').click(function() {
		createDialog("Processing your download request, please wait. This will take some time for large benchmarks.");
		token=Math.floor(Math.random()*100000000);
		$('#downLink').attr('href', starexecRoot+"secure/download?token=" +token+ "&type=bench&id="+$("#benchId").attr("value"));
		destroyOnReturn(token);
	});
	
	
});


