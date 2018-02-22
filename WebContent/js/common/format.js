"use strict";

window.star = window.star || {};
window.star.format = {};

(function(format) {

	var linkTemplate = document.createElement("a");
	linkTemplate.target = "_blank";

	format.link = function(url, text) {
		linkTemplate.href = url;
		linkTemplate.textContent = text;
		return linkTemplate.outerHTML;
	};

	var jobTemplate = [starexecRoot + "secure/details/job.jsp?id=", null];
	format.jobUrl = function(jobId) {
		jobTemplate[1] = jobId;
		return jobTemplate.join("");
	};
	format.jobLink = function(job) {
		return format.link(
			format.jobUrl(job["id"]),
			job["name"]
		);
	};

	var userTemplate = [starexecRoot + "secure/details/user.jsp?id=", null];
	format.userUrl = function(userId) {
		userTemplate[1] = userId;
		return userTemplate.join("");
	};
	format.userLink = function(user) {
		return format.link(
			format.userUrl(user["id"]),
			user["name"]
		);
	};

	format.timestamp = function(time) {
		return (new Date(time)).toISOString()
		.replace("T", " ")
		.substring(0, 16);
	};

	var heatcolorConfig = {
		maxval: 100,
		minval: 0,
		colorStyle: 'greentored',
		lightness: 0
	};
	var heatcolorTemplate = ["<div class='stat'>", null, " %</div>"];
	format.heatcolor = function(percent) {
		heatcolorTemplate[1] = parseInt(percent, 10);
		return $(heatcolorTemplate.join(""))
		.heatcolor(
			function() {return percent},
			heatcolorConfig
		)
		.wrapAll("<div>")
		.parent()
		.html()
			;
	}

})(window.star.format);
