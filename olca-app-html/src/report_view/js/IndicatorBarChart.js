
IndicatorBarChart = function() {

	return function(scope, elem, atts) {

		var chart;

		scope.$watch('selectedBarChartIndicator', function() {
			if (!scope.selectedBarChartIndicator) {
				return;
			}
			if (chart) {
				updateChart(chart, scope);
			} else {
				var data = getChartData(scope);
				var canvas = elem.get(0);
				var dist = 400 / (2 * scope.report.variants.length);
				if (dist < 10)
					dist = 5;
				chart = new Chart(canvas.getContext("2d")).Bar(data, {
					barValueSpacing: dist,
					tooltipTemplate: "<%if (label){%><%=label%>: <%}%><%= value.toExponential(2) %>",
					multiTooltipTemplate: "<%= value.toExponential(2) %>"
				});
			}
		});

		function getChartData(scope) {
			var labels = [];
			var data = [];
			var indicator = scope.selectedBarChartIndicator;
			var variants = scope.report.variants;
			for (var i = 0; i < variants.length; i++) {
				labels.push(variants[i].name);
				data.push(scope.getVariantResult(variants[i], indicator));
			}
			return {
				labels: labels,
				datasets: [
					{
						label: indicator.reportName,
						fillColor: "rgba(151,187,205,0.5)",
						strokeColor: "rgba(151,187,205,0.8)",
						highlightFill: "rgba(151,187,205,0.75)",
						highlightStroke: "rgba(151,187,205,1)",
						data: data
					}
				]
			};
		}

		function updateChart(chart, scope) {
			var indicator = scope.selectedBarChartIndicator;
			var variants = scope.report.variants;
			for (var i = 0; i < variants.length; i++) {
				var val = scope.getVariantResult(variants[i], indicator);
				chart.datasets[0].bars[i].value = val;
			}
			chart.update();
		}
	};
};

