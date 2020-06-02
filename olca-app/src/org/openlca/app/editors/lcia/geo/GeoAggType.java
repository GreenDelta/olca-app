package org.openlca.app.editors.lcia.geo;

import org.openlca.app.M;

/**
 * 
 * Defines how multiple values of a parameter in different geographic features
 * are aggregated.
 */
enum GeoAggType {

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
