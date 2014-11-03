
// Creates a radar or bar chart with all indicator results for each variant
// relative to each other or normalised to the normalisation values of the
// respective indicators. The chart type is passed as value of the directive
// attribute. The following values are supported:
// relative_bar, normalisation_bar, relative_radar, normalisation_radar
NormalisedIndicatorChart = function() {

	return function(scope, elem, atts) {
		var canvas = elem.get(0);
		var report = scope.report;
		var type = atts["normalisedIndicatorChart"];
		if (!type) {
			console.log("no chart type given in normalisedIndicatorChart directive");
			return;
		}
		var data = calculateDatasets(report, type);
		if (type === 'relative_radar' || type === 'normalisation_radar') {
			new Chart(canvas.getContext("2d")).Radar(data, {
				tooltipTemplate: "<%if (label){%><%=label%>: <%}%><%= value.toExponential(2) %>",
				multiTooltipTemplate: "<%= value.toExponential(2) %>"
			});
		} else if (type === 'relative_bar' || type === 'normalisation_bar') {
			var dist = 400 / (2 * report.variants.length * scope.getDisplayedIndicators().length);
			if (dist < 5)
				dist = 5;
			new Chart(canvas.getContext("2d")).Bar(data, {
				barValueSpacing: dist,
				tooltipTemplate: "<%if (label){%><%=label%>: <%}%><%= value.toExponential(2) %>",
				multiTooltipTemplate: "<%= value.toExponential(2) %>"
			});
		}
	};

	function calculateDatasets(report, type) {
		var dataSets = [];
		var index = getResultIndex(report);
		for (var i = 0; i < report.variants.length; i++) {
			var variant = report.variants[i];
			var data = [];
			for (var j = 0; j < report.indicators.length; j++) {
				var indicator = report.indicators[j];
				if (!indicator.displayed) {
					continue;
				}
				var indexEntry = index[indicator.id];
				var val = indexEntry.variantResults[variant.name];
				if (type === 'normalisation_radar' || type === 'normalisation_bar') {
					var factor = indicator.normalisationFactor;
					if(!factor) {
						data.push(0);
					} else {
						data.push(val / factor);
					}
				} else {
					var max = indexEntry.max;
					if(!max) {
						data.push(0);
					} else {
						data.push(100 * val / max);
					}
				}
			}
			var rgb = Colors.getPredefinedRgb(i);
			var colorPref = "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + ",";
			dataSets.push(createDataSet(variant, colorPref, data, type));
		}
		return {
			labels: getIndicatorLabels(report),
			datasets: dataSets};
	}

	function createDataSet(variant, colorPref, data, type) {
		if (type === 'relative_radar' || type === 'normalisation_radar') {
			return {
				label: variant.name,
				fillColor: colorPref + "0.2)",
				strokeColor: colorPref + ",1)",
				pointColor: colorPref + "1)",
				pointStrokeColor: "#fff",
				pointHighlightFill: "#fff",
				pointHighlightStroke: colorPref + "1)",
				data: data
			};
		} else {
			return {
				label: variant.name,
				fillColor: colorPref + "0.5)",
				strokeColor: colorPref + "0.8)",
				highlightFill: colorPref + "0.75)",
				highlightStroke: colorPref + "1)",
				data: data
			};
		}
	}

	function getIndicatorLabels(report) {
		var labels = [];
		for (var i = 0; i < report.indicators.length; i++) {
			var indicator = report.indicators[i];
			if (!indicator.displayed) {
				continue;
			}
			labels.push(indicator.reportName);
		}
		return labels;
	}

	// returns an object with the maximum and variant results for each indicator
	function getResultIndex(report) {
		var index = {};
		for (var i = 0; i < report.results.length; i++) {
			var result = report.results[i];
			var max = null;
			var varResults = {};
			for (var j = 0; j < result.variantResults.length; j++) {
				var varResult = result.variantResults[j];
				var total = varResult.totalAmount;
				varResults[varResult.variant] = total;
				if (!max) {
					max = Math.abs(total);
				} else {
					max = Math.max(max, Math.abs(total));
				}
			}
			var indexEntry = {
				max: max,
				variantResults: varResults
			};
			index[result.indicatorId] = indexEntry;
		}
		return index;
	}

};