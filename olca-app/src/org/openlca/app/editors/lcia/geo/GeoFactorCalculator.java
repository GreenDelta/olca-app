package org.openlca.app.editors.lcia.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Location;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.geo.calc.IntersectionCalculator;
import org.openlca.geo.geojson.Feature;
import org.openlca.geo.geojson.FeatureCollection;
import org.openlca.geo.geojson.MsgPack;
import org.openlca.util.BinUtils;
import org.openlca.util.Pair;
import org.openlca.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongDoubleHashMap;
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

		// calculate the intersections
		FeatureCollection coll = setup.getFeatures();
		if (coll == null || coll.features.isEmpty()) {
			log.error("no features available for the "
					+ "intersection calculation");
			return;
		}

		clearFactors();
		TLongDoubleHashMap defaults = calcDefaultValues();

//		IntersectionCalculator calc = IntersectionCalculator.on(coll);
//		LocationDao locDao = new LocationDao(db);
//		List<Pair<Location, List<Pair<Feature, Double>>>> intersections = locDao.getAll()
//				.parallelStream()
//				.map(loc -> Pair.of(loc, getIntersections(loc, calc)))
//				.collect(Collectors.toList());
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

	/**
	 * Calculates the default characterization factors for the flows that are bound
	 * in the setup. For the calculation of these factors the default values of the
	 * geo-parameters are used. The value of the default characterization factor is
	 * used for the factor with no location binding and for factors with location
	 * bindings but without any intersecting features with parameter values.
	 */
	private TLongDoubleHashMap calcDefaultValues() {
		TLongDoubleHashMap defaults = new TLongDoubleHashMap();

		// init the formula interpreter
		FormulaInterpreter fi = new FormulaInterpreter();
		for (GeoParam param : setup.params) {
			fi.bind(param.identifier,
					Double.toString(param.defaultValue));
		}

		// calculate the default factors
		for (GeoFlowBinding b : setup.bindings) {
			if (b.flow == null)
				continue;
			try {
				double val = fi.eval(b.formula);
				defaults.put(b.flow.id, val);
				ImpactFactor f = impact.addFactor(b.flow);
				f.value = val;
			} catch (Exception e) {
				log.error("failed to evaluate formula {} "
						+ " of binding with flow {}", b.formula, b.flow);
			}
		}

		return defaults;
	}

	/**
	 * Calculates the parameter values for the given locations from the respective
	 * intersections with the given feature collection and the aggregation function
	 * that is defined in the respective parameter.
	 */
	private List<Triple<Location, GeoParam, Double>> calcParamVals(
			List<Location> locations, FeatureCollection coll) {
		IntersectionCalculator calc = IntersectionCalculator.on(coll);
		Map<Location, List<Pair<Feature, Double>>> map = locations.stream()
				.map(loc -> Pair.of(loc, calcIntersections(loc, calc)))
				.collect(Collectors.toMap(p -> p.first, p -> p.second));

		List<Triple<Location, GeoParam, Double>> triples = new ArrayList<>();
		map.forEach((loc, pairs) -> {
			for (GeoParam param : setup.params) {
				if (pairs.isEmpty()) {
					triples.add(Triple.of(loc, param, null));
					continue;
				}

				List<Double> vals = new ArrayList<>();
				List<Double> shares = new ArrayList<>();
				for (Pair<Feature, Double> pair : pairs) {
					Feature f = pair.first;
					Double share = pair.second;
					if (f.properties == null)
						continue;
					Object valObj = f.properties.get(param.name);
					if (!(valObj instanceof Number))
						continue;
					vals.add(((Number) valObj).doubleValue());
					shares.add(share);
				}
				Double aggVal = aggregate(param, vals, shares);
				triples.add(Triple.of(loc, param, aggVal));

			}
		});
		return triples;
	}

	/**
	 * Calculates the intersection of the given location.
	 */
	private List<Pair<Feature, Double>> calcIntersections(
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

	private Double aggregate(GeoParam param,
			List<Double> vals, List<Double> shares) {

		return null;
	}

}
