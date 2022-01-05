package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

public record Ec3ImpactResult(
	String method, List<Ec3IndicatorResult> indicatorResults) {

	public static Ec3ImpactResult of(String method) {
		return new Ec3ImpactResult(method, new ArrayList<>());
	}

}
