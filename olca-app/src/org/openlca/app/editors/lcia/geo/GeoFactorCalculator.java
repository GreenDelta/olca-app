package org.openlca.app.editors.lcia.geo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.LocationDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Location;
import org.openlca.geo.calc.IntersectionCalculator;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.MsgPack;
import org.openlca.util.BinUtils;
import org.openlca.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.set.hash.TLongHashSet;

class GeoFactorCalculator implements Runnable {

	private final Setup setup;
	private final ImpactCategory impact;
	private final Logger log = LoggerFactory.getLogger(getClass());

	GeoFactorCalculator(Setup setup, ImpactCategory impact) {
		this.setup = setup;
		this.impact = impact;
	}

	@Override
	public void run() {

		// check the input
		if (setup == null || impact == null) {
			log.error("no setup or LCIA category");
			return;
		}
		IDatabase db = Database.get();
		if (db == null) {
			log.error("no connected database");
			return;
		}
		if (setup.bindings.isEmpty()) {
			log.warn("no flow bindings; nothing to do");
			return;
		}

		// initialize the intersection calculator
		FeatureCollection coll = setup.getFeatures();
		if (coll == null || coll.features.isEmpty()) {
			log.error("no features available for the "
					+ "intersection calculation");
			return;
		}
		IntersectionCalculator calc = IntersectionCalculator.on(coll);
		LocationDao locDao = new LocationDao(db);
		List<Pair<Location, List<Pair<Feature, Double>>>> intersections = locDao.getAll()
				.parallelStream()
				.map(loc -> Pair.of(loc, getIntersections(loc, calc)))
				.collect(Collectors.toList());
	}

	/**
	 * Remove the factors for the flows that are part of the setup from the LCIA
	 * category.
	 */
	private void clearFactors() {
		TLongHashSet setupFlows = new TLongHashSet();
		for (GeoFlowBinding b : setup.bindings) {
			if (b.flow == null)
				continue;
			setupFlows.add(b.flow.id);
		}
		impact.impactFactors.removeIf(
				f -> f.flow != null && setupFlows.contains(f.flow.id));
	}

	private List<Pair<Feature, Double>> getIntersections(
			Location loc, IntersectionCalculator calc) {
		if (loc.geodata == null) {
			log.info("No geodata for location {} found", loc);
			return Collections.emptyList();
		}
		try {
			byte[] data = BinUtils.gunzip(loc.geodata);
			if (data == null) {
				log.info("No geodata for location {} found", loc);
				return Collections.emptyList();
			}
			FeatureCollection coll = MsgPack.unpack(data);
			if (coll == null || coll.features.isEmpty()) {
				log.info("No geodata for location {} found", loc);
				return Collections.emptyList();
			}
			Feature f = coll.features.get(0);
			if (f == null || f.geometry == null) {
				log.info("No geodata for location {} found", loc);
				return Collections.emptyList();
			}

			List<Pair<Feature, Double>> s = calc.shares(f.geometry);
			log.trace("Calculated intersetions for location {}", loc);
			return s;
		} catch (Exception e) {
			log.error("Failed to calculate the "
					+ "intersections for location " + loc, e);
			return Collections.emptyList();
		}
	}

}
