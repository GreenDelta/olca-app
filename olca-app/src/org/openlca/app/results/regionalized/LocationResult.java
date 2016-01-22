package org.openlca.app.results.regionalized;

import org.openlca.geo.kml.KmlFeature;

class LocationResult {

	final KmlFeature kmlFeature;
	final long locationId;
	double amount;

	LocationResult(KmlFeature feature, long locationId) {
		this.kmlFeature = feature;
		this.locationId = locationId;
	}

}
