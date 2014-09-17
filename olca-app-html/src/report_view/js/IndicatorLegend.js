
indicatorLegend = function() {
	
	return function(scope, elem, atts) {
		var element = $(elem.get(0));
		$.each(scope.getDisplayedIndicators(), function(i, indicator){
			var color = Colors.getPredefinedString(i);
			var text = getElementText(indicator.reportName, color);
			element.append(text);
		});			
	};
	
	function getElementText(name, color) {
		return '<span style="padding: 10px">' +
				'<span class="glyphicon glyphicon-stop" style="color: ' + 
				color + '; padding: 3px"></span>' +
				name + '</span>';
	}
	
};



