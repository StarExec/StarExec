function doSubmit(){
	$('#btnSubmit').text('Uploading');
	$('#btnSubmit').attr('disabled', 'disabled');
	$('form').submit();
}