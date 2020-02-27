package org.openlca.app.editors.lcia.geo;

class GeoParam {

	/** The name of the parameter in the GeoJSON file. */
	String name;

	/** The identifier of the parameter for usage in formulas */
	String identifier;

	/** The minimum value of the parameter in the features of the GeoJSON file. */
	double min;

	/** The maximum value of the parameter in the features of the GeoJSON file. */
	double max;

	/**
	 * The default value that should be taken in the calculation of regionalized
	 * factors when no intersecting geometry was found.
	 */
	double defaultValue;

	/**
	 * Defines how multiple values of this parameter in different geographic
	 * features are aggregated.
	 */
	GeoAggType aggType;

}
