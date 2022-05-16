package org.openlca.app.tools.openepd.export;

import java.util.ArrayList;
import java.util.List;

import org.openlca.io.openepd.EpdDoc;
import org.openlca.io.openepd.EpdImpactResult;
import org.openlca.io.openepd.EpdIndicatorResult;
import org.openlca.io.openepd.EpdMeasurement;
import org.openlca.io.openepd.EpdScopeValue;
import org.openlca.io.openepd.Vocab;
import org.openlca.util.Strings;

public record MappedResult(
	List<EpdImpactResult> impacts,
	List<EpdIndicatorResult> resources,
	List<EpdIndicatorResult> outputs
) {

	public static MappedResult of(List<MappingModel> models) {
		var result = new MappedResult(
			new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
		if (models == null)
			return result;
		for (var model : models) {
			for (var row : model.rows()) {
				var indicator = row.epdIndicator();
				if (indicator == null)
					continue;
				var r = EpdIndicatorResult.of(indicator.code());
				for (var scope : model.scopes()) {
					var v = row.values().get(scope);
					if (v == null)
						continue;
					r.values().add(new EpdScopeValue(
							scope, EpdMeasurement.of(v, indicator.unit())));
				}
				var list = switch (row.epdIndicator().type()) {
					case LCI_IN -> result.resources;
					case LCI_OUT -> result.outputs;
					case LCIA -> result.getOrCreate(model.epdMethod()).results();
				};
				list.add(r);
			}
		}
		return result;
	}

	private EpdImpactResult getOrCreate(Vocab.Method method) {
		var m = method != null
			? method
			: Vocab.Method.UNKNOWN_LCIA;
		for (var r : impacts) {
			if (Strings.nullOrEqual(r.method(), m.code()))
				return r;
		}
		var r = EpdImpactResult.of(m.code());
		impacts.add(r);
		return r;
	}

	public void applyOn(EpdDoc doc) {
		if (doc == null)
			return;
		doc.impactResults.clear();
		doc.impactResults.addAll(impacts);
		doc.resourceUses.clear();
		doc.resourceUses.addAll(resources);
		doc.outputFlows.clear();
		doc.outputFlows.addAll(outputs);
	}
}
