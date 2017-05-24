"use strict";

jQuery(function($) {
	var resultsTableConfig = new window.star.DataTableConfig({
		"paging": false
	});
	var resultsTable = $("#analytics_results").DataTable(resultsTableConfig);

	var $startField = $("#dateselector [name='start']");
	var $endField   = $("#dateselector [name='end']"  );

	var resultsTableRefresh = function(data, textStatus, jqXHR) {
		resultsTable.clear();
		var row = data.length;
		while (--row != -1) {
			resultsTable.row.add([
				data[row]["event"],
				data[row]["count"],
				data[row]["users"],
			]);
		}
		resultsTable.draw();
	};

	$("#dateselector").on("submit", function(event) {
		$.get(
			starexecRoot+"services/analytics",
			{
				"start": $startField.val(),
				"end": $endField.val()
			},
			resultsTableRefresh
		);

		event.preventDefault();
	});

	$("#dateselector").trigger("submit");

	(function(){
		var data = [1,2,3,4,5,6,7,8,7,4];
		var timeline = d3.select("#analytics_timeline");
		var yScale = d3.scaleLinear()
			.domain([0, d3.max(data)])
			.range([0, 300]);
		var xScale = d3.scaleLinear()
			.domain([0, data.length])
			.range([0, 968]);

		timeline
			.selectAll("div")
			.data(data)
			.enter().append("div")
			.classed("bar", function() { return true; })
			.style("height", function(d) { return yScale(d) + "px"; })
			.style("width",  function(d) { return xScale(1) + "px"; })
		;
	})();
});
