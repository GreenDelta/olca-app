package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

/** A location item in the tree. */
class LocationItem {

	final Contribution<Location> contribution;
	final List<Contribution<ProcessDescriptor>> processContributions = new ArrayList<>();

	LocationItem(Contribution<Location> contribution) {
		this.contribution = contribution;
	}

}