package org.openlca.app.editors.sd.interop;

import org.openlca.core.model.Parameter;
import org.openlca.sd.eqn.Id;

public class VarBinding {

	private Parameter parameter;
	private Id varId;

	public Parameter parameter() {
		return parameter;
	}

	public VarBinding parameter(Parameter parameter) {
		this.parameter = parameter;
		return this;
	}

	public Id varId() {
		return varId;
	}

	public VarBinding varId(Id varId) {
		this.varId = varId;
		return this;
	}
}
