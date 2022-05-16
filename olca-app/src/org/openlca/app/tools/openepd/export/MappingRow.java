package org.openlca.app.tools.openepd.export;

import org.openlca.core.model.ImpactCategory;
import org.openlca.io.openepd.Vocab;

import java.util.HashMap;
import java.util.Map;

class MappingRow {
	final ImpactCategory indicator;
	final Map<String, Double> values = new HashMap<>();

	Vocab.Indicator epdIndicator;
	Vocab.UnitMatch unitMatch;
	double factor = 1.0;

	MappingRow(ImpactCategory indicator) {
		this.indicator = indicator;
	}

}
