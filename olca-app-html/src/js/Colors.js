(function() {
	"use strict";

	var root = this;

	var Colors = function() {

		var predefColors = [
			"#E53039",
			"#296FC4",
			"#FFC923",
			"#52A84D",
			"#844CAD",
			"#7FB7E5",
			"#FF8900",
			"#359B58",
			"#319444",
			"#FCFF64"
		];

		var predefRGBs = [];
		for (var i = 0; i < predefColors.length; i++) {
			var col = predefColors[i];
			var r = parseInt(col.substring(1, 3), 16);
			var g = parseInt(col.substring(3, 5), 16);
			var b = parseInt(col.substring(5, 7), 16);
			predefRGBs.push({r: r, g: g, b: b});
		}

		this.getPredefinedString = function(index) {
			if (index < predefColors.length) {
				return predefColors[index];
			} else {
				return "#000000";
			}
		};

		this.getPredefinedRgb = function(index) {
			if (index < predefRGBs.length) {
				return predefRGBs[index];
			} else {
				return {r: 0, g: 0, b: 0};
			}
		};
	};
	
	root.Colors = new Colors();
	
}).call(this);

