package org.openlca.app.results.localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openlca.core.math.Table;
import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;

/**
 * A localised impact assessment result contains results for the categories of
 * an impact assessment method result for different locations. There are two
 * values for each location-impact-assessment-category pair stored: a result
 * where no localised impact assessment factors were applied ('result') and a
 * result where localised impact assessment factors were applied
 * ('localisedResult'). The localisedResult should be equal to the normal result
 * if no localised impact assessment factors were applied.
 */
public class LocalisedImpactResult {

	private ImpactMethodDescriptor impactMethod;
	private Table<ImpactCategoryDescriptor, Location> results;
	private Table<ImpactCategoryDescriptor, Location> localisedResults;

	public LocalisedImpactResult(ImpactMethodDescriptor impactMethod) {
		this.impactMethod = impactMethod;
		results = new Table<>(ImpactCategoryDescriptor.class, Location.class);
		localisedResults = new Table<>(ImpactCategoryDescriptor.class,
				Location.class);
	}

	public ImpactMethodDescriptor getImpactMethod() {
		return impactMethod;
	}

	public ImpactCategoryDescriptor[] getImpactCategories() {
		return results.getRows();
	}

	public Location[] getLocations() {
		return results.getColumns();
	}

	/** Add up the results with the values in the given entry. */
	void addUp(Entry entry) {
		if (!entry.isValid())
			return;
		double old = results.getEntry(entry.impactCategory, entry.location);
		results.setEntry(entry.impactCategory, entry.location, old
				+ entry.result);
		double oldLocal = localisedResults.getEntry(entry.impactCategory,
				entry.location);
		localisedResults.setEntry(entry.impactCategory, entry.location,
				oldLocal + entry.localResult);
	}

	public List<Entry> getEntries(ImpactCategoryDescriptor impact) {
		Map<Location, Double> vals = results.getColumnEntries(impact);
		Map<Location, Double> localVals = localisedResults
				.getColumnEntries(impact);
		List<Entry> entries = new ArrayList<>();
		for (Location location : vals.keySet()) {
			Entry e = new Entry();
			e.impactCategory = impact;
			e.location = location;
			Double val = vals.get(location);
			Double localVal = localVals.get(location);
			e.result = val == null ? 0 : val;
			e.localResult = localVal == null ? 0 : localVal;
			entries.add(e);
		}
		return entries;
	}

	public static class Entry {

		private ImpactCategoryDescriptor impactCategory;
		private Location location;
		private double result;
		private double localResult;

		boolean isValid() {
			return impactCategory != null && location != null;
		}

		public ImpactCategoryDescriptor getImpactCategory() {
			return impactCategory;
		}

		public void setImpactCategory(ImpactCategoryDescriptor impactCategory) {
			this.impactCategory = impactCategory;
		}

		public Location getLocation() {
			return location;
		}

		public void setLocation(Location location) {
			this.location = location;
		}

		public double getResult() {
			return result;
		}

		public void setResult(double result) {
			this.result = result;
		}

		public double getLocalResult() {
			return localResult;
		}

		public void setLocalResult(double localResult) {
			this.localResult = localResult;
		}
	}

}
