package org.openlca.app.editors.results;

import org.openlca.app.db.Database;
import org.openlca.core.model.Flow;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ResultFlow;
import org.openlca.core.model.ResultImpact;
import org.openlca.core.model.ResultModel;
import org.openlca.core.model.ResultOrigin;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class Util {

	private Util() {
	}

	static boolean canAddImpact(
		ResultModel result, CategorizedDescriptor impact) {
		if (result == null || impact == null)
			return false;

		// false if the impact is already in the results
		for (var ir : result.impacts) {
			if (ir.indicator != null && ir.indicator.id == impact.id)
				return false;
		}

		// false if the result has an assigned impact method
		// and the given impact is not part of that method
		var method = result.setup != null
			? result.setup.impactMethod()
			: null;
		if (method == null)
			return true;
		for (var i : method.impactCategories) {
			if (i.id == impact.id)
				return true;
		}
		return false;
	}

	static boolean addImpact(ResultModel result, CategorizedDescriptor d) {
		var db = Database.get();
		if (db == null || result == null || d == null)
			return false;
		var impact = db.get(ImpactCategory.class, d.id);
		if (impact == null)
			return false;
		var ri = new ResultImpact();
		ri.indicator = impact;
		ri.amount = 1.0;
		ri.origin = ResultOrigin.ENTERED;
		return result.impacts.add(ri);
	}

	static boolean addFlow(ResultModel r, CategorizedDescriptor d, boolean input) {
		var db = Database.get();
		if (db == null || r == null || d == null)
			return false;
		var flow = db.get(Flow.class, d.id);
		if (flow == null)
			return false;
		var rf = new ResultFlow();
		rf.flow = flow;
		rf.flowPropertyFactor = flow.getReferenceFactor();
		rf.unit = flow.getReferenceUnit();
		rf.isInput = input;
		rf.amount = 1.0;
		rf.origin = ResultOrigin.ENTERED;
		return r.inventory.add(rf);
	}

}
