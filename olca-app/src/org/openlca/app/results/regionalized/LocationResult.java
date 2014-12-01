package org.openlca.app.results.regionalized;

import org.openlca.geo.kml.KmlFeature;

class LocationResult {

	private KmlFeature kmlFeature;
	private long locationId;
	private double totalAmount;

	LocationResult(KmlFeature feature, long locationId) {
		this.kmlFeature = feature;
		this.locationId = locationId;
	}

	public void addAmount(double amount) {
		totalAmount += amount;
	}

	public double getTotalAmount() {
		return totalAmount;
	}

	public KmlFeature getKmlFeature() {
		return kmlFeature;
	}

	public long getLocationId() {
		return locationId;
	}

}
