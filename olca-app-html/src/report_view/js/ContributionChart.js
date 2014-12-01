
ContributionChart = function() {

	return function(scope, elem, atts) {

		var chart;

		scope.$watch('selectedContributionIndicator', function() {
			if (!scope.selectedContributionIndicator) {
				return;
			}
			if (chart) {
				updateChart(chart, scope);
			} else {
				var data = createChartData(scope);
				var canvas = elem.get(0);
				var dist = 400 / (2 * scope.report.variants.length);
				if (dist < 5)
					dist = 5;
				chart = new Chart(canvas.getContext("2d")).StackedBar(data, {
					barDatasetSpacing: dist, 
					barValueSpacing: dist,
					tooltipTemplate: "<%if (label){%><%=label%>: <%}%><%= value.toExponential(2) %>",
					multiTooltipTemplate: "<%= value.toExponential(2) %>"
				});
			}

		});
	};

	function createChartData(scope) {
		var labels = [];
		var dataSets = initDataSets(scope.report.processes);
		var indicator = scope.selectedContributionIndicator;
		$.each(scope.report.variants, function(i, variant) {
			labels.push(variant.name);
			var contributions = getContributions(scope, indicator, variant);
			$.each(contributions, function(i, contribution) {
				var idx = getIndex(contribution, scope.report.processes);
				if (idx < 0)
					return;
				dataSets[idx].data.push(contribution.amount);
			});
		});
		return {
			labels: labels,
			datasets: dataSets
		};
	}
	
	function updateChart(chart, scope) {
		var indicator = scope.selectedContributionIndicator;
		$.each(scope.report.variants, function(i, variant) {			
			var contributions = getContributions(scope, indicator, variant);
			$.each(contributions, function(j, contribution) {
				var idx = getIndex(contribution, scope.report.processes);
				if (idx < 0)
					return;				
				chart.datasets[idx].bars[i].value = contribution.amount;
			});
		});
		chart.update();
	}

	function getContributions(scope, indicator, variant) {
		for (var i = 0; i < scope.report.results.length; i++) {
			var result = scope.report.results[i];
			if (result.indicatorId !== indicator.id) {
				continue;
			}
			for (var j = 0; j < result.variantResults.length; j++) {
				var varResult = result.variantResults[j];
				if (varResult.variant === variant.name) {
					return varResult.contributions;
				}
			}
		}
		return [];
	}

	function initDataSets(processes) {
		var dataSets = [];
		$.each(processes, function(i, process) {
			var color = Colors.getPredefinedRgb(i);
			var pref = "rgba(" + color.r + "," + color.g + "," + color.b + ",";
			var datSet = {
				label: process.reportName,
				fillColor: pref + "0.5)",
				strokeColor: pref + "0.8)",
				highlightFill: pref + "0.75)",
				highlightStroke: pref + "1)",
				data: []
			};
			dataSets.push(datSet);
		});
		dataSets.push({
			label: "Other",
			fillColor: "rgba(211,211,211,0.5)",
			strokeColor: "rgba(211,211,211,0.8)",
			highlightFill: "rgba(211,211,211,0.75)",
			highlightStroke: "rgba(211,211,211,1)",
			data: []
		});
		return dataSets;
	}

	function getIndex(contribution, processes) {
		if (contribution.rest)
			return processes.length;
		for (var i = 0; i < processes.length; i++) {
			if (contribution.processId === processes[i].id) {
				return i;
			}
		}
		return -1;
	}
};
