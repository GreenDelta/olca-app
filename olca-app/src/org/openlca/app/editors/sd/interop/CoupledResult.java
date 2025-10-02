package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.matrix.index.ImpactIndex;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.LcaResult;
import org.openlca.sd.eqn.Id;
import org.openlca.sd.eqn.SimulationState;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.cells.NumCell;

public class CoupledResult {

	private final Map<Id, Var> vars = new HashMap<>();
	private Map<Id, List<Double>> varResults = new HashMap<>();
	private final List<Double> time = new ArrayList<>();

	private ImpactIndex impactIdx;
	private List<Double>[] impactResults;

	public boolean hasImpactResults() {
		return impactIdx != null
				&& !impactIdx.isEmpty()
				&& impactResults != null;
	}

	public List<ImpactDescriptor> getImpactCategories() {
		if (impactIdx == null)
			return List.of();
		var iis = new ArrayList<ImpactDescriptor>();
		for (var i : impactIdx) {
			iis.add(i);
		}
		return iis;
	}

	public List<Var> getVariables() {
		return new ArrayList<>(vars.values());
	}

	public double[] impactResultsOf(ImpactDescriptor indicator) {
		if (indicator == null || impactIdx == null)
			return new double[time.size()];
		int i = impactIdx.of(indicator);
		if (i < 0 || i >= impactResults.length || impactResults[i] == null)
			return new double[time.size()];
		var list = impactResults[i];
		var array = new double[list.size()];
		for (int k = 0; k < list.size(); k++) {
			var v = list.get(k);
			array[k] = v == null ? 0 : v;
		}
		return array;
	}

	public double[] varResultsOf(Var variable) {
		if (variable == null)
			return new double[time.size()];
		var list = varResults.get(variable.name());
		if (list == null)
			return new double[time.size()];
		var array = new double[list.size()];
		for (int k = 0; k < list.size(); k++) {
			var v = list.get(k);
			array[k] = v == null ? 0 : v;
		}
		return array;
	}

	void append(SimulationState state, List<LcaResult> rs) {
		if (state == null)
			return;
		time.add(state.time());
		appendVarResults(state);
		appendResults(rs);
	}

	private void appendVarResults(SimulationState state) {
		var map = state.vars();
		if (map == null)
			return;
		for (var v : map.values()) {
			if (!(v.value() instanceof NumCell(double num)))
				continue;
			vars.putIfAbsent(v.name(), v);
			varResults.computeIfAbsent(v.name(), $ -> new ArrayList<>())
					.add(num);
		}
	}

	@SuppressWarnings("unchecked")
	private void appendResults(List<LcaResult> rs) {
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

		var results = new double[impactIdx.size()];
		for (var r : rs) {
			if (!r.hasImpacts())
				continue;
			for (int i = 0; i < impactIdx.size(); i++) {
				var indicator = impactIdx.at(i);
				var value = r.getTotalImpactValueOf(indicator);
				results[i] += value;
			}
		}

		if (impactResults == null) {
			impactResults = new List[impactIdx.size()];
		}
		for (int i = 0; i < impactIdx.size(); i++) {
			if (impactResults[i] == null) {
				impactResults[i] = new ArrayList<>();
			}
			impactResults[i].add(results[i]);
		}
	}

}
