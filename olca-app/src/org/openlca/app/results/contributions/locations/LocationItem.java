package org.openlca.app.results.contributions.locations;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Location;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.ContributionItem;

/** A location item in the tree. */
class LocationItem {

	final ContributionItem<Location> contribution;
	final List<ContributionItem<ProcessDescriptor>> processContributions = new ArrayList<>();

	LocationItem(ContributionItem<Location> contribution) {
		this.contribution = contribution;
	}

}