package org.openlca.app.editors.lcia.geo;

import org.openlca.core.model.Flow;

/**
 * Describes the binding of regionalized characterization factors of a flow via
 * a formula with parameters of geographic features.
 */
class GeoFlowBinding {
	final Flow flow;
	String formula;

	GeoFlowBinding(Flow flow) {
		this.flow = flow;
		this.formula = "1";
	}
}
