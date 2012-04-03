$(document).ready(function(){

	$('#fieldSites').expandable(true);			
});

var flag = true;

function changeImage(obj){
	
	if (flag){
		var splitString = obj.src.split("&type=");
		obj.src = splitString[0] + ("&type=sorg");
		obj.width = 600;
	} else{
		var splitString = obj.src.split("&type=");
		obj.src = splitString[0] + ("&type=sthn");
		obj.width = 150;
	}
	flag = !flag;
}