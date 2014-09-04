var Controller = function($scope) {
	
	$scope.report = {};

	$scope.getColorString = function(index) {
		return Colors.getPredefinedString(index);
	};

	$scope.getIndicatorName = function(indicatorId) {
		var indicators = $scope.report.indicators;
		for (var i = 0; i < indicators.length; i++) {
			var indicator = indicators[i];
			if (indicator.id === indicatorId) {
				return indicator.reportName;
			}
		}
		return "";
	};

	$scope.getVariantResult = function(variant, indicator) {
		var results = $scope.report.results;
		for (var i = 0; i < results.length; i++) {
			var result = results[i];
			if (result.indicatorId !== indicator.id)
				continue;
			for (var j = 0; j < result.variantResults.length; j++) {
				var variantResult = result.variantResults[j];
				if (variantResult.variant === variant.name)
					return variantResult.totalAmount;
			}
		}
		return 0;
	};

	$scope.getSingleScore = function(variant) {
		var score = 0;
		for (var i = 0; i < $scope.report.indicators.length; i++) {
			var indicator = $scope.report.indicators[i];
			if (!indicator.displayed)
				continue;
			var nFactor = indicator.normalisationFactor;
			var wFactor = indicator.weightingFactor;
			for (var j = 0; j < $scope.report.results.length; j++) {
				var result = $scope.report.results[j];
				if (result.indicatorId !== indicator.id)
					continue;
				for (var k = 0; k < result.variantResults.length; k++) {
					var vr = result.variantResults[k];
					if (vr.variant !== variant.name)
						continue;
					score = score + (vr.totalAmount * wFactor) / nFactor;
				}
			}
		}
		return score;
	};
	
	$scope.getDisplayedIndicators = function() {
		if(!($scope.report.indicators))
			return [];
		var displayed = [];
		var indicators = $scope.report.indicators;
		for(var i = 0; i < indicators.length; i++) {
			if(indicators[i].displayed) {
				displayed.push(indicators[i]);
			}				
		}
		return displayed;
	};
};

