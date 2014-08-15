$(document).ready(function(){
	initUI();
	attachFormValidation();
});


/**
 * Setup the user interface buttons & actions
 */
function initUI() {
	$('.submitBtn').button({
		icons:{
			secondary: "ui-icon-disk"
		}
	});
	$(".cancelBtn").button({
		icons: {
			secondary: "ui-icon-closethick"
		}
	});
	
	//redirect to the testing page on cancel
	$('.cancelBtn').click(function(){
		window.location = starexecRoot+"secure/admin/testing.jsp";
	});
}

/**
 * Attaches validation to the stress test form
 */
function attachFormValidation() {
	
	// Add validation to the configuration save form
	$("#saveConfigForm").validate({
		rules: {
			spaceCount: {
				required: true,
				min: 0
			},
			jobCount: {
				required: true,
				min: 0
			},
			userCount: {
				required: true,
				min: 0
			},
			minUsersPer: {
				required: true,
				min: 0
			},
			maxUsersPer: {
				required: true,
				min: 0
			},
			minSolversPer: {
				required: true,
				min: 0
			},
			maxSolversPer: {
				required: true,
				min: 0
			},
			minBenchmarksPer: {
				required: true,
				min: 0
			},
			maxBenchmarksPer: {
				required: true,
				min: 0
			},
			spacesPerJob: {
				required: true,
				min: 0
			}
		},
		messages: {
			spaceCount: {
			required: "field required",
			min: "minimum value of 0"
		},
		jobCount: {
			required: "field required",
			min: "minimum value of 0"
		},
		userCount: {
			required: "field required",
			min: "minimum value of 0"
		},
		minUsersPer: {
			required: "field required",
			min: "minimum value of 0"
		},
		maxUsersPer: {
			required: "field required",
			min: "minimum value of 0"
		},
		minSolversPer: {
			required: "field required",
			min: "minimum value of 0"
		},
		maxSolversPer: {
			required: "field required",
			min: "minimum value of 0"
		},
		minBenchmarksPer: {
			required: "field required",
			min: "minimum value of 0"
		},
		maxBenchmarksPer: {
			required: "field required",
			min: "minimum value of 0"
		},
		spacesPerJob: {
			required: "field required",
			min: "minimum value of 0"
		}
		}
	});
}