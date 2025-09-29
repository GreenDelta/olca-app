package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.List;

import org.openlca.ilcd.methods.ImpactMethod;

public class SimulationSetup {

	private ImpactMethod method;
	private final List<SystemBinding> systemBindings = new ArrayList<>();

	public ImpactMethod method() {
		return method;
	}

	public SimulationSetup method(ImpactMethod method) {
		this.method = method;
		return this;
	}

	public List<SystemBinding> systemBindings() {
		return systemBindings;
	}
}
