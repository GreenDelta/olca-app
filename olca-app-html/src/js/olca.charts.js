var olca = olca || {};

olca.BarChart = function(divId, width, height) {

	var self = this;
	this.divId = divId;
	this.width = width;
	this.height = height;

	// Bar area: area where the bars a drawn
	this.offsetTop = 20;
	this.offsetLeft = 20;
	this.offsetRight = 0;
	this.offsetBottom = 30;
	this.barAreaHeight = this.height - this.offsetTop - this.offsetBottom;
	this.barAreaWidth = this.width - this.offsetLeft - this.offsetRight;

	// default value initialisation
	this.data = [];
	this.bars = null;
	this.valueFetchFn = null;
	this.labelFetchFn = null;
	this.segmentWidth = 50;
	this.barWidth = 30;
	this.maxAbsPositive = 0;
	this.maxAbsNegative = 0;
	this.maxAbs = 0;
	this.zeroLineY = this.height - this.offsetBottom;

	// calculates the general chart data like segment or bar width
	this.calculateChartData = function() {
		if(!self.data)
			return;
		self.segmentWidth = self.barAreaWidth / self.data.length;
		self.barWidth = self.calculateBarWidth(self.segmentWidth, 30);

		// minimum & maximum
		self.maxAbsPositive = 0;
		self.maxAbsNegative = 0;		
		for(var i = 0; i < self.data.length; i++) {
			var value = self.fetchValue(self.data[i]);
			if(value >= 0)
				self.maxAbsPositive = Math.max(self.maxAbsPositive, value);
			else
				self.maxAbsNegative = Math.max(self.maxAbsNegative, Math.abs(value));
		}
		self.maxAbs = Math.max(self.maxAbsPositive, self.maxAbsNegative);

		// zero line
		if(self.maxAbsNegative === 0)
			self.zeroLineY = self.height - self.offsetBottom;
		else {
			var positiveShare = self.maxAbsPositive / (self.maxAbsPositive + self.maxAbsNegative);
			self.zeroLineY = self.barAreaHeight * positiveShare + self.offsetTop;
		}

	};

	// calculates the optimal bar-with
	this.calculateBarWidth = function(segmentWidth, maxBarWidth) {
		if((segmentWidth - 2) > maxBarWidth)
			return maxBarWidth;
		if(segmentWidth < 3)
			return 1;
		else
			return segmentWidth - 2;
	};

	// calculates the top-left x-position for the bar of datum i
	this.barX = function(datum, i) {
		return self.offsetLeft 
				+ (i * self.segmentWidth) 
				+ self.segmentWidth / 2 
				- self.barWidth / 2;
	};

	// calculates the top-left y-position for the bar of datum i
	this.barY = function(datum, i) {
		var value = self.fetchValue(datum);
		if(value <= 0)
			return self.zeroLineY;
		var barHeightVal = self.barHeight(datum, i);
		return self.zeroLineY - barHeightVal;
	};

	// calculates the bar height for the datum i
	this.barHeight = function(datum, i) {
		var value = self.fetchValue(datum);
		if(value === 0)
			return 0;
		if(value > 0) {
			if(self.maxAbsPositive === 0)
				return 0;
			var share = value / self.maxAbsPositive;
			var distance = self.zeroLineY - self.offsetTop;
			return share * distance;			
		} else {
			if(self.maxAbsNegative === 0)
				return 0;
			var share = Math.abs(value) / self.maxAbsNegative;
			var distance = self.height - self.offsetBottom - self.zeroLineY;
			return share * distance;
		}		
	};

	// get the value of the data point
	this.fetchValue = function(datum) {
		if(!self.valueFetchFn)
			return datum;
		else
			return self.valueFetchFn(datum);
	};

	// get the label of the data point
	this.fetchLabel = function(datum) {
		if(!self.labelFetchFn)
			return '';
		else
			return self.labelFetchFn(datum);
	};

	// the bar color for data point i
	this.barColor = function(datum, i) {
		var value = self.fetchValue(datum);
		if(self.maxAbs === 0)
			return 0;
		var share = Math.abs(value) / self.maxAbs;
		var red, green, blue;
		if(value >= 0) {
			red = 255 * share;
			green = Math.sqrt(red * (255 - red));
			blue = 255 - red;			
		} else {
			green = 255 * share;
			red = Math.sqrt(green * (255 - green));
			blue = 255 - green;
		}
		return d3.rgb(red, green, blue);
	};

	// get the x-position of the label for datum i
	this.labelX = function(datum, i) {
		return self.offsetLeft + self.segmentWidth * i + self.segmentWidth / 2;
	};

	this.render = function(data, valueFetchFn, labelFetchFn) {

		self.valueFetchFn = valueFetchFn;
		self.labelFetchFn = labelFetchFn;
		self.data = data;
		self.calculateChartData();

		// remove old svg and append new
		d3.select(self.divId)
			.select('svg')
			.remove();
		var svg = d3.select(self.divId)
					.append('svg')
					.attr('width', self.width)
					.attr('height', self.height);

		self.bars = svg.selectAll("rect")
					.data(data)
					.enter()
					.append("rect");

		self.bars.attr("x", self.barX)
					.attr("y", self.barY)
					.attr("width", self.barWidth)
					.attr("height", self.barHeight)
					.attr("fill", self.barColor);

		// data labels if this function is provided
		if(!self.labelFetchFn)
			return;					
		svg.selectAll("text")
			.data(data)
			.enter()
			.append("text")
			.text(self.labelFetchFn)
			.attr("x", self.labelX)
			.attr("y", (self.height - self.offsetBottom / 2))
			.attr("text-anchor", "middle");
	};

	this.transition = function(valueFetchFn) {
		self.valueFetchFn = valueFetchFn;
		if (!self.bars)
			return;
		self.calculateChartData();
		self.bars.transition()
			.duration(500)
			.delay(function(d, i) { return 200 * i/self.data.length; })
			.attr("y", self.barY)
			.attr("height", self.barHeight)
			.attr("fill", self.barColor);
	};
};