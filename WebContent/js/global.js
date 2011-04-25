$(function(){		
	$('.round').corner('10px');
	
	$('.btn').hover(
		function(){$(this).addClass("ui-state-hover");},
		function(){$(this).removeClass("ui-state-hover");}
	).mousedown(function(){
		$(this).addClass("ui-state-active");	
	})
	.mouseup(function(){
		$(this).removeClass("ui-state-active");
	});
	
	setActivePage($(location).attr('href'));
});		

function setActivePage(path){
	if(path.indexOf('uploadbench') >= 0){
		$('#benchmark a').addClass('current');
	} else if(path.indexOf('uploadsolver') >= 0){
		$('#solver a').addClass('current');
	} else if(path.indexOf('createjob') >= 0){
		$('#job a').addClass('current');
	} else {
		$('#dashboard a').addClass('current');
	} 
}