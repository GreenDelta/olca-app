package org.openlca.app.editors.lcia.geo;

import org.openlca.app.M;

/**
 * Defines how the values of a numeric property of set of geometric features
 * are aggregated.
 */
enum GeoAggregation {

	WEIGHTED_AVERAGE,

	AVERAGE,

	MINIMUM,

	MAXIMUM;

	@Override
	public String toString() {
		switch (this) {
		case WEIGHTED_AVERAGE:
			return M.WeightedAverage;
		case AVERAGE:
			return "Average";
		case MINIMUM:
			return M.Minimum;
		case MAXIMUM:
			return M.Maximum;
		default:
			return "?";
		}
	}
}
