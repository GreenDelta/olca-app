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
	 * Defines how multiple values of this parameter in different geographic
	 * features are aggregated.
	 */
	GeoAggType aggType;

}
