
variantLegend = function() {
	
	return function(scope, elem, atts) {
		var element = $(elem.get(0));
		for(var i = 0; i < scope.report.variants.length; i++) {
			var variant = scope.report.variants[i];
			var color = Colors.getPredefinedString(i);
			var text = getElementText(variant.name, color);
			element.append(text);
		}		
	};
	
	function getElementText(variantName, color) {
		return '<span style="padding: 10px">' +
				'<span class="glyphicon glyphicon-stop" style="color: ' + 
				color + '; padding: 3px"></span>' +
				variantName + '</span>';
	}
	
};
