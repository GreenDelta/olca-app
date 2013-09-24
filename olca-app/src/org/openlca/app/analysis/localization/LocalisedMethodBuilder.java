package org.openlca.app.analysis.localization;

import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.Descriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a localised impact assessment method (template) from a standard impact
 * assessment method. It searches for the location with the code 'GLO' in the
 * database and creates default assessment factors for this location. If no such
 * location is available no factors are created.
 */
public class LocalisedMethodBuilder {

	private IDatabase database;
	private ImpactMethod method;
	private Location defaultLocation;

	public LocalisedMethodBuilder(ImpactMethod method, IDatabase database) {
		this.method = method;
		this.database = database;
	}

	public LocalisedImpactMethod build() {
		if (method == null)
			return null;
		LocalisedImpactMethod locMethod = new LocalisedImpactMethod();
		locMethod.setImpactMethod(Descriptors.toDescriptor(method));
		this.defaultLocation = findDefaultLocation(database);
		if (defaultLocation != null)
			locMethod.getLocations().add(defaultLocation);
		addCategories(locMethod);
		return locMethod;
	}

	private Location findDefaultLocation(IDatabase database) {
		try {
			List<Location> locations = database.createDao(Location.class)
					.getAll();
			for (Location location : locations)
				if ("glo".equalsIgnoreCase(location.getCode()))
					return unwrap(location);
			return null;
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to find default location", e);
			return null;
		}
	}

	/**
	 * Copying the location instance is important to avoid cyclic references
	 * when serialising JPA managed entities to JSON.
	 */
	private Location unwrap(Location location) {
		if (location == null)
			return null;
		Location unwrapped = new Location();
		unwrapped.setName(location.getName());
		unwrapped.setCode(location.getCode());
		unwrapped.setLatitude(location.getLatitude());
		unwrapped.setLongitude(location.getLongitude());
		unwrapped.setDescription(location.getDescription());
		unwrapped.setId(location.getId());
		return unwrapped;
	}

	private void addCategories(LocalisedImpactMethod locMethod) {
		for (ImpactCategory category : method.getImpactCategories()) {
			LocalisedImpactCategory locCategory = new LocalisedImpactCategory();
			locMethod.getImpactCategories().add(locCategory);
			locCategory.setImpactCategory(Descriptors.toDescriptor(category));
			if (defaultLocation == null)
				continue;
			addFactors(category, locCategory);
		}
	}

	private void addFactors(ImpactCategory category,
			LocalisedImpactCategory locCategory) {
		for (ImpactFactor factor : category.getImpactFactors()) {
			LocalisedImpactFactor locFactor = new LocalisedImpactFactor();
			locCategory.getFactors().add(locFactor);
			locFactor.setFlow(Descriptors.toDescriptor(factor.getFlow()));
			locFactor.addValue(defaultLocation, convertedValue(factor));
		}
	}

	private double convertedValue(ImpactFactor factor) {
		if (factor == null || factor.getUnit() == null)
			return 0;
		double f = factor.getValue() * factor.getUnit().getConversionFactor();
		if (factor.getFlowPropertyFactor() == null
				|| factor.getFlowPropertyFactor().getConversionFactor() == 0)
			return f;
		else
			return f / factor.getFlowPropertyFactor().getConversionFactor();
	}

}
