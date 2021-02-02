package org.openlca.app.editors.lcia.geo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;
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
import org.openlca.geo.geojson.GeoJSON;
import org.openlca.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.map.hash.TLongByteHashMap;
import gnu.trove.set.hash.TLongHashSet;

class GeoFactorCalculator implements Runnable {

	private final Setup setup;
	private final ImpactCategory impact;
	private final List<Location> locations;
	private final Logger log = LoggerFactory.getLogger(getClass());

	GeoFactorCalculator(
			Setup setup, ImpactCategory impact, List<Location> locations) {
		this.setup = setup;
		this.impact = impact;
		this.locations = locations;
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

		// calculate the intersections, parameter values,
		// and finally generate the LCIA factors
		if (setup.features.isEmpty()) {
			log.error("no features available for the "
					+ "intersection calculation");
			return;
		}
		var params = calcParamVals(setup.features);
		createFactors(params);
	}

	/**
	 * Calculates the parameter values for the given locations from the respective
	 * intersections with the given feature collection and the aggregation function
	 * that is defined in the respective parameter.
	 */
	private Map<Location, List<Pair<GeoProperty, Double>>> calcParamVals(
			FeatureCollection coll) {
		IntersectionCalculator calc = IntersectionCalculator.on(coll);
		Map<Location, List<Pair<Feature, Double>>> map = locations
				.parallelStream()
				.map(loc -> Pair.of(loc, calcIntersections(loc, calc)))
				.collect(Collectors.toMap(p -> p.first, p -> p.second));

		Map<Location, List<Pair<GeoProperty, Double>>> locParams = new HashMap<>();
		map.forEach((loc, pairs) -> {
			List<Pair<GeoProperty, Double>> paramVals = new ArrayList<>();
			locParams.put(loc, paramVals);
			for (GeoProperty param : setup.properties) {
				if (pairs.isEmpty()) {
					paramVals.add(Pair.of(param, null));
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
				paramVals.add(Pair.of(param, aggVal));

			}
		});
		return locParams;
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
			FeatureCollection coll = GeoJSON.unpack(loc.geodata);
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

	/**
	 * Aggregates the given parameter values that were extracted from the
	 * intersecting features with the aggregation function that is defined in the
	 * given parameter. If the lists are empty `null` is returned which means that
	 * the default parameter value should be used in this case. The shares are only
	 * used when a weighted average should be calculated, which is the default
	 * aggregation function. Note that the shares must have the same length as the
	 * corresponding parameter values.
	 */
	private Double aggregate(
		GeoProperty param, List<Double> vals, List<Double> shares) {

		if (param == null || vals.isEmpty()) {
			return null;
		}

		// take the minimum or maximum value
		if (param.aggregation == GeoAggregation.MINIMUM
				|| param.aggregation == GeoAggregation.MAXIMUM) {

			DoubleBinaryOperator fn = param.aggregation == GeoAggregation.MINIMUM
					? Math::min
					: Math::max;

			double val = vals.get(0) == null ? 0 : vals.get(0);
			for (int i = 1; i < vals.size(); i++) {
				Double next = vals.get(i);
				if (next == null)
					continue;
				val = fn.applyAsDouble(val, next);
			}
			return val;
		}

		// calculate the average value
		if (param.aggregation == GeoAggregation.AVERAGE) {

			double sum = 0;
			int count = 0;
			for (int i = 0; i < vals.size(); i++) {
				Double next = vals.get(i);
				if (next == null)
					continue;
				sum += next;
				count++;
			}

			if (count == 0)
				return null;
			return sum / count;
		}

		// calculate the weighted average by default
		double sum = 0;
		double wsum = 0;
		for (int i = 0; i < vals.size(); i++) {
			Double next = vals.get(i);
			Double share = shares.get(i);
			if (next == null || share == null)
				continue;
			sum += next * share;
			wsum += share;
		}
		if (wsum == 0) {
			return null;
		}
		return sum / wsum;

	}

	private void createFactors(
			Map<Location, List<Pair<GeoProperty, Double>>> locParams) {

		// remove all LCIA factors with a flow and location
		// that will be calculated
		TLongHashSet setupFlows = new TLongHashSet();
		for (GeoFlowBinding b : setup.bindings) {
			if (b.flow == null)
				continue;
			setupFlows.add(b.flow.id);
		}
		TLongHashSet setupLocations = new TLongHashSet();
		for (Location loc : locations) {
			setupLocations.add(loc.id);
		}
		TLongByteHashMap isDefaultPresent = new TLongByteHashMap();
		List<ImpactFactor> removals = new ArrayList<>();
		for (ImpactFactor factor : impact.impactFactors) {
			if (factor.flow == null)
				return;
			long flowID = factor.flow.id;
			if (!setupFlows.contains(flowID))
				continue;
			if (factor.location == null) {
				isDefaultPresent.put(flowID, (byte) 1);
			} else if (setupLocations.contains(factor.location.id)) {
				removals.add(factor);
			}
		}
		impact.impactFactors.removeAll(removals);

		// generate the non-regionalized default factors
		// for setup flows that are not yet present
		FormulaInterpreter fi = new FormulaInterpreter();
		for (GeoProperty param : setup.properties) {
			fi.bind(param.identifier,
					Double.toString(param.defaultValue));
		}
		for (GeoFlowBinding b : setup.bindings) {
			if (b.flow == null)
				continue;
			byte present = isDefaultPresent.get(b.flow.id);
			if (present == (byte) 1)
				continue;
			try {
				double val = fi.eval(b.formula);
				impact.factor(b.flow, val);
			} catch (Exception e) {
				log.error("failed to evaluate formula {} "
						+ " of binding with flow {}", b.formula, b.flow);
			}
		}

		// finally, generate regionalized factors
		for (Location loc : locParams.keySet()) {

			// bind the location specific parameter values
			// to a formula interpreter
			fi = new FormulaInterpreter();
			List<Pair<GeoProperty, Double>> pairs = locParams.get(loc);
			if (pairs == null)
				continue;
			for (Pair<GeoProperty, Double> pair : pairs) {
				GeoProperty param = pair.first;
				double val = pair.second == null
						? param.defaultValue
						: pair.second;
				fi.bind(param.identifier, Double.toString(val));
			}

			for (GeoFlowBinding b : setup.bindings) {
				if (b.flow == null || b.formula == null)
					continue;
				try {
					double val = fi.eval(b.formula);
					var factor = impact.factor(b.flow, val);
					factor.location = loc;
				} catch (Exception e) {
					log.error("Failed to calculate factor from formula "
							+ b.formula + " in binding with flow " + b.flow, e);
				}
			}
		}
	}
}
