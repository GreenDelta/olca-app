package org.openlca.core.editors.model;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Location;

/**
 * Model for a localised characterisation factor. The flow property and unit of
 * the flow are currently not stored so it is assumed that the factors are
 * provided per reference flow property and unit of the flow.
 */
public class LocalisedImpactFactor implements Comparable<LocalisedImpactFactor> {

	private FlowInfo flow;
	private List<Location> locations = new ArrayList<>();
	private List<Double> factors = new ArrayList<>();

	public FlowInfo getFlow() {
		return flow;
	}

	public void setFlow(FlowInfo flow) {
		this.flow = flow;
	}

	public void addValue(Location location, double factor) {
		if (location == null || factor == 0)
			return;
		locations.add(location);
		factors.add(factor);
	}

	public double getValue(Location location) {
		if (location == null)
			return 0d;
		for (int i = 0; i < locations.size(); i++) {
			Location stored = locations.get(i);
			if (location.equals(stored))
				return factors.get(i);
		}
		return 0;
	}

	@Override
	public int compareTo(LocalisedImpactFactor other) {
		if (other == null)
			return 1;
		if (flow != null && other.flow != null)
			return flow.compareTo(other.flow);
		return 0;
	}
}
