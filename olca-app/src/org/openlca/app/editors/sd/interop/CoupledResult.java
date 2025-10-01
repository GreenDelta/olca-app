package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.matrix.index.ImpactIndex;
import org.openlca.core.results.LcaResult;
import org.openlca.sd.eqn.Id;
import org.openlca.sd.eqn.SimulationState;
import org.openlca.sd.eqn.Var;

public class CoupledResult {

	private final Map<Id, Var> vars = new HashMap<>();
	private Map<Id, List<Double>> varResults = new HashMap<>();
	private final List<Double> time = new ArrayList<>();

	private ImpactIndex impactIdx;
	private List<Double>[] impactResults;

	void add(SimulationState state) {
		if (state == null)
			return;
	}

	void add(List<LcaResult> rs) {
		if (rs == null || rs.isEmpty())
			return;

		if (impactIdx == null) {
			for (var r : rs) {
				if (r.hasImpacts()) {
					impactIdx = r.impactIndex();
					break;
				}
			}
		}
		if (impactIdx == null)
			return;

		for (var r : rs) {
			if (!r.hasImpacts())
				continue;
			if (impactIdx == null) {
				impactIdx = r.impactIndex();
			}
		}
	}
}
