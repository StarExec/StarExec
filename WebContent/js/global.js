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
	buildSubmenu();	
});		

function setActivePage(path){
	if(path.indexOf('uploadbench') >= 0){
		$('#benchmark a').addClass('current');
	} else if(path.indexOf('uploadsolver') >= 0){
		$('#solver a').addClass('current');
	} else if(path.indexOf('createjob') >= 0){
		$('#cjob a').addClass('current');
	} else if(path.indexOf('viewjob') >= 0){
		$('#vjob a').addClass('current');
	} else {
		$('#dashboard a').addClass('current');
	} 
}

function buildSubmenu(){
	$('#job a').hover(function(){
		// Mouse in
		$('#job a').parent().append('<li id="jobView"><a href="createjob.jsp"><span>View</span></a></li>');
		$('#job a').parent().append('<li id="jobCreate"><a href="createjob.jsp"><span>Create</span></a></li>');
	},
	function(){
		$('#jobView').remove();
		$('#jobCreate').remove();
	});
}