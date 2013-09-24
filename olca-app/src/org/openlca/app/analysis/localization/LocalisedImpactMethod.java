package org.openlca.app.analysis.localization;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

/**
 * A localised impact assessment method contains impact assessment categories
 * with characterisation factors for different locations.
 */
public class LocalisedImpactMethod {

	private ImpactMethodDescriptor impactMethod;
	private List<Location> locations = new ArrayList<>();
	private List<LocalisedImpactCategory> impactCategories = new ArrayList<>();
	private Location defaultLocation;

	public ImpactMethodDescriptor getImpactMethod() {
		return impactMethod;
	}

	public void setImpactMethod(ImpactMethodDescriptor impactMethod) {
		this.impactMethod = impactMethod;
	}

	public List<Location> getLocations() {
		return locations;
	}

	public List<LocalisedImpactCategory> getImpactCategories() {
		return impactCategories;
	}

	public void setDefaultLocation(Location defaultLocation) {
		this.defaultLocation = defaultLocation;
	}

	public Location getDefaultLocation() {
		if (defaultLocation == null) {
			for (Location location : locations) {
				if ("glo".equalsIgnoreCase(location.getCode())) {
					defaultLocation = location;
					break;
				}
			}
		}
		return defaultLocation;
	}

}
