(function() {
	"use strict";

	var root = this;

	var Colors = function() {

		var predefColors = [
			"#00b1f1",
			"#800080",
			"#ff9900",
			"#a46141",
			"#52a84d",
			"#e53039",
			"#125985",
			"#844cad",
			"#ffc923",
			"#70bb28",
			"#7fb7e5",
			"#ff8900",
			"#359b58",
			"#319444",
			"#fcff64"
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

