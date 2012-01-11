// When the document is ready to be executed on
$(document).ready(function(){
	$('#tblSpace tbody').children('tr:even').addClass('shade');
	$('#tblSpace tbody').children('tr:last-child').removeClass('shade');
	
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: 32
			},
			desc: {
				maxlength: 1024
			}
		},
		messages: {
			name:{
				required: "enter a space name",
				minlength: ">= 2 characters",
				maxlength: "< 32 characters"
			},
			desc: {
				maxlength: "< 1024 characters"
			}
		}
	});
	
	$('#btnCreate').button({
		icons: {
			secondary: "ui-icon-plus"
    }});
});