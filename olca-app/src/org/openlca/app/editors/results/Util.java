package org.openlca.app.editors.results;

import org.openlca.app.db.Database;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowResult;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactResult;
import org.openlca.core.model.Result;
import org.openlca.core.model.descriptors.RootDescriptor;

class Util {

	private Util() {
	}

	static boolean canAddImpact(Result result, RootDescriptor impact) {
		if (result == null || impact == null)
			return false;

		// false if the impact is already in the results
		for (var ir : result.impactResults) {
			if (ir.indicator != null && ir.indicator.id == impact.id)
				return false;
		}

		// false if the result has an assigned impact method
		// and the given impact is not part of that method
		if (result.impactMethod == null)
			return true;
		for (var i : result.impactMethod.impactCategories) {
			if (i.id == impact.id)
				return true;
		}
		return false;
	}

	static boolean addImpact(Result result, RootDescriptor d) {
		var db = Database.get();
		if (db == null || result == null || d == null)
			return false;
		var impact = db.get(ImpactCategory.class, d.id);
		if (impact == null)
			return false;
		var ri = new ImpactResult();
		ri.indicator = impact;
		ri.amount = 1.0;
		return result.impactResults.add(ri);
	}

	static boolean addFlow(Result r, RootDescriptor d, boolean input) {
		var db = Database.get();
		if (db == null || r == null || d == null)
			return false;
		var flow = db.get(Flow.class, d.id);
		if (flow == null)
			return false;
		var rf = new FlowResult();
		rf.flow = flow;
		rf.flowPropertyFactor = flow.getReferenceFactor();
		rf.unit = flow.getReferenceUnit();
		rf.isInput = input;
		rf.amount = 1.0;
		return r.flowResults.add(rf);
	}

}
