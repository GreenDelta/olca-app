package org.openlca.app.editors.sd.results;

record ChartRange(double min, double max) {

	static ChartRange of(double[] trace) {
		if (trace == null || trace.length == 0)
			return new ChartRange(0, 1);

		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (double x : trace) {
			min = Math.min(x, min);
			max = Math.max(x, max);
		}
		return min == 0 && max == 0
				? new ChartRange(0, 1)
				: new ChartRange(lower(min), upper(max));
	}

	static ChartRange ofAll(double[]... traces) {
		if (traces == null || traces.length == 0)
			return new ChartRange(0, 1);
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (var trace : traces) {
			var scale = of(trace);
			min = Math.min(scale.min, min);
			max = Math.max(scale.max, max);
		}
		return new ChartRange(min, max);
	}

	private static double upper(double x) {
		if (x <= 0)
			return 0;
		double base = Math.floor(Math.log10(x)) - 1;
		double factor = Math.pow(10, base);
		return Math.ceil(x / factor) * factor;
	}

	private static double lower(double x) {
		return x >= 0
				? 0
				: -upper(Math.abs(x));
	}

}
