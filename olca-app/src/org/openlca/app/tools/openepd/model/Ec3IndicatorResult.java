package org.openlca.app.tools.openepd.model;

import java.util.ArrayList;
import java.util.List;

public record Ec3IndicatorResult(String indicator, List<Ec3ScopeValue> values) {

	public static Ec3IndicatorResult of(String indicator) {
		return new Ec3IndicatorResult(indicator, new ArrayList<>());
	}

}
