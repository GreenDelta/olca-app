package org.openlca.app.editors.sd.interop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.model.ImpactMethod;
import org.openlca.sd.eqn.Id;

public class SimulationSetup {

	private ImpactMethod method;
	private final List<SystemBinding> systemBindings = new ArrayList<>();
	private final Map<Id, Rect> positions = new HashMap<>();

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

	public Map<Id, Rect> positions() {
		return positions;
	}
}
