package org.openlca.app.tools.params;

import java.util.Objects;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Strings;

class Param {

	final ParameterRedef redef;
	final Descriptor context;
	double start;
	double end;

	private Param(ParameterRedef redef, Descriptor context) {
		this.redef = redef;
		this.context = context;
		this.start = redef.value;
		this.end = redef.value;
	}

	static Param of(ParameterRedef redef, IDatabase db) {
		Descriptor context = null;
		if (redef.contextId != null) {
			context = redef.contextType == ModelType.IMPACT_CATEGORY
					? db.getDescriptor(ImpactCategory.class, redef.contextId)
					: db.getDescriptor(Process.class, redef.contextId);
		}
		return new Param(redef, context);
	}

	boolean hasRedef(ParameterRedef r) {
		if (redef == null || r == null)
			return false;
		return Objects.equals(redef.name, r.name)
				&& Objects.equals(redef.contextId, r.contextId);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof Param other))
			return false;
		return this.hasRedef(other.redef);
	}
}
