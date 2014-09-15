
SingleScoreBarChart = function() {

	return function(scope, elem, atts) {
		var canvas = elem.get(0);
		var report = scope.report;
		var dist = 400 / (2 * report.variants.length);
		if (dist < 5)
			dist = 5;
		var data = createChartData(scope);
		new Chart(canvas.getContext("2d")).StackedBar(data, {
			barDatasetSpacing: dist, 
			barValueSpacing: dist,
			tooltipTemplate: "<%if (label){%><%=label%>: <%}%><%= value.toExponential(2) %>",
			multiTooltipTemplate: "<%= value.toExponential(2) %>"
		});
	};

	function createChartData(scope) {
		var report = scope.report;
		var dataSets = createDataSets(scope);
		var variantNames = [];
		$.each(report.variants, function(i, variant) {
			variantNames.push(variant.name);
			$.each(scope.getDisplayedIndicators(), function(j, indicator) {
				var result = scope.getVariantResult(variant, indicator);
				var nFactor = indicator.normalisationFactor;
				var wFactor = indicator.weightingFactor;
				var score = result * wFactor / nFactor;
				dataSets[j].data.push(score);
			});
		});
		return {
			labels: variantNames,
			datasets: dataSets
		};
	}

	// initializes an empty data set (series) for every indicator
	function createDataSets(scope) {
		var dataSets = [];
		$.each(scope.getDisplayedIndicators(), function(i, indicator) {
			var color = Colors.getPredefinedRgb(i);
			var pref = "rgba(" + color.r + "," + color.g + "," + color.b + ",";
			var dataSet = {
				label:indicator.name,
				fillColor: pref + "0.5)",
				strokeColor: pref + "0.8)",
				highlightFill: pref + "0.75)",
				highlightStroke: pref + "1)",
				data: []
			};
			dataSets.push(dataSet);
		});
		return dataSets;
	}

};
