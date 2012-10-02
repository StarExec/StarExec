// Variables for keeping state in the 3-step process of job creation
var commId = 3;


$(document).ready(function(){
	
	initUI();
	attachFormValidation();	
	
});


/**
 * Attach validation to the job creation form
 * TODO: adapt for singlejobpair page
 */
function attachFormValidation(){
	
	// Add regular expression capabilities to the validator
	$.validator.addMethod(
			"regex", 
			function(value, element, regexp) {
				var re = new RegExp(regexp);
				return this.optional(element) || re.test(value);
	});
	
	// Set up form validation
	$("#addForm").validate({
		rules: {
			name: {
				required: true,
				minlength: 2,
				maxlength: 32,
				regex : getPrimNameRegex()
			},
			desc: {
				required: true,
				maxlength: 1024,
				regex: getPrimDescRegex()
			},
			cpuTimeout: {
				required: true,			    
			    max: 259200
			},
			wallclockTimeout: {
				required: true,			    
			    max: 259200
			},
			queue: {
				required: true
			}
		},
		messages: {
			name:{
				required: "enter a job name",
				minlength: "2 characters minimum",
				maxlength: "32 characters maximum",
				regex: "invalid character(s)"
			},
			desc: {
				required: "enter a job description",
				maxlength: "1024 characters maximum",
				regex: "invalid character(s)"
			},
			cpuTimeout: {
				required: "enter a timeout",			    
			    max: "3 day max timeout"
			},
			wallclockTimeout: {
				required: "enter a timeout",			    
			    max: "3 day max timeout"
			},
			queue: {
				required: "error - no worker queues"
			}
		},
		// Place the error messages in the tooltip instead of in the DOM
		errorPlacement: function (error, element) {
			if($(error).text().length > 0){
				$(element).qtip('api').updateContent('<b>'+$(error).text()+'</b>', true);
			}
		},
		// Hide the error tooltip when no errors are present
		success: function(label){
			$('#' + $(label).attr('for')).qtip('api').hide();
		}
	});
};


/**
 * Sets up the jQuery button style and attaches click handlers to those buttons.
 */
function initUI() {

    $("#publicCommunity").change(function () {
    	var comId = $(this).val();
        var str = "";
        
        $("#publicCommunity option:selected").each(function () {
              str += $(this).text() + comId + " ";
              
              $.getJSON('/starexec/services/communities/solvers/' + comId,{}, function(result){
            	  var box = "<select id='publicSolver'>";
            	  
            	  for (var i = 0; i < result.length; i++){
            		  
            		  box += "<option value = '" + result[i][0] + "'>" + result[i][1] + "</option>";
            	  }
            	  box += "</select>";
            	  $('#publicSolver').replaceWith(box);
              });
              
            });
       
        $("benchmarkContents").text(str);   
    
    })
      .trigger('change');
}