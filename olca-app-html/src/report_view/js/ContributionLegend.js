
contributionLegend = function() {
	
	return function(scope, elem, atts) {
		var element = $(elem.get(0));
		$.each(scope.report.processes, function(i, process){
			var color = Colors.getPredefinedString(i);
			var text = getElementText(process.reportName, color);
			element.append(text);
		});
		var otherText = getElementText("Other", "#d3d3d3");
		element.append(otherText);
	};
	
	function getElementText(name, color) {
		return '<span style="padding: 10px">' +
				'<span class="glyphicon glyphicon-stop" style="color: ' + 
				color + '; padding: 3px"></span>' +
				name + '</span>';
	}
	
};


