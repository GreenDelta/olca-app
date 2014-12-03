var Initializer = function(report, messages) {

	var element = document.getElementById("contentDiv");
	var scope = angular.element(element).scope();
	sortLists(report);
	scope.$apply(function() {
		scope.report = report;
		scope.Messages = messages;
		var initialIndicator = getFirstDisplayedIndicator(report);
		scope.selectedContributionIndicator = initialIndicator;
		scope.selectedBarChartIndicator = initialIndicator;
	});

	function sortLists(report) {
		report.variants.sort(function(v1, v2) {
			if (!v1.name || !v2.name)
				return 0;
			return v1.name.localeCompare(v2.name);
		});
		report.indicators.sort(function(i1, i2) {
			if (!i1.reportName || !i2.reportName)
				return 0;
			return i1.reportName.localeCompare(i2.reportName);
		});
		report.processes.sort(function(p1, p2) {
			if(!p1.reportName || !p2.reportName)
				return 0;
			return p1.reportName.localeCompare(p2.reportName);
		});
	}

	function getFirstDisplayedIndicator(report) {
		if (!report.indicators) {
			return null;
		}
		for (var i = 0; i < report.indicators.length; i++) {
			var indicator = report.indicators[i];
			if (indicator.displayed) {
				return indicator;
			}
		}
		return null;
	}

};

