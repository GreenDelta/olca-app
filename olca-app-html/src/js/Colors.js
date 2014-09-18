(function() {
	"use strict";

	var root = this;

	var Colors = function() {

		var predefColors = [
			"#e53039",
			"#296fc4",
			"#ffc923",
			"#52a84d",
			"#844cad",
			"#7fb7e5",
			"#ff8900",
			"#800080",
			"#874c3f",
			"#fcff64",
			"#00b1f1",
			"#70bb28",
			"#125985",
			"#e20073",
			"#ffff55",
			"#da0018",
			"#006f9a",
			"#ff9900"
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

