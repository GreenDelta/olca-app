package org.openlca.app.tools.openepd.export;

import org.openlca.core.model.ImpactCategory;
import org.openlca.io.openepd.Vocab;

import java.util.HashMap;
import java.util.Map;

public class MappingRow {

	private final Map<String, Double> values = new HashMap<>();

	private ImpactCategory indicator;
	private Vocab.Indicator epdIndicator;
	private Vocab.UnitMatch unit;
	private double factor = 1.0;

	public ImpactCategory indicator() {
		return indicator;
	}

	public MappingRow indicator(ImpactCategory indicator) {
		this.indicator = indicator;
		return this;
	}

	public Vocab.Indicator epdIndicator() {
		return epdIndicator;
	}

	public MappingRow epdIndicator(Vocab.Indicator epdIndicator) {
		this.epdIndicator = epdIndicator;
		return this;
	}

	public Vocab.UnitMatch unit() {
		return unit;
	}

	public MappingRow unit(Vocab.UnitMatch unitMatch) {
		this.unit = unitMatch;
		return this;
	}

	public Map<String, Double> values() {
		return values;
	}

	public double factor() {
		return factor;
	}

	public MappingRow factor(double factor) {
		this.factor = factor;
		return this;
	}
}
