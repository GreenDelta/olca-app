package org.openlca.app.results.slca;

import org.openlca.core.model.RiskLevel;

public class SocialRiskValue {

	// values() allocates an array each time it is called
	// but there is no easy other way to get the number
	// of constants of an enum type, thus we cache it
	private static final int N = RiskLevel.values().length;

	private final double[] values = new double[N];

	public int size() {
		return N;
	}

	public void put(RiskLevel level, double value) {
		if (level == null)
			return;
		values[level.ordinal()] = value;
	}

	public void put(int level, double value) {
		values[level] = value;
	}

	public void add(int level, double value) {
		values[level] += value;
	}

	public double get(RiskLevel level) {
		return level != null
				? values[level.ordinal()]
				: 0;
	}

	public double get(int level) {
		return values[level];
	}

	public double getShare(RiskLevel level) {
		return level != null
				? getShare(level.ordinal())
				: 0;
	}

	public double getShare(int level) {
		double s = sum();
		return s != 0
				? Math.abs(get(level) / s)
				: 0;
	}

	private double sum() {
		double s = 0;
		for (double d : values) {
			s += d;
		}
		return s;
	}
}
