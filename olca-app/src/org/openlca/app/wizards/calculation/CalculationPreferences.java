package org.openlca.app.wizards.calculation;

import com.google.gson.JsonObject;
import org.openlca.app.db.DatabaseDir;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.data_quality.AggregationType;
import org.openlca.core.math.data_quality.NAHandling;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.CalculationSetup;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ImpactMethod;
import org.openlca.jsonld.Json;
import org.openlca.util.Dirs;

import java.io.File;

class CalculationPreferences {

	private static final String FILE = "calculation-preferences.json";

	private CalculationType type;
	private AllocationMethod allocation;
	private String impactMethod;
	private String nwSet;
	private Integer simulationRuns;
	private boolean withRegionalization;
	private boolean withCosts;
	private boolean withDataQuality;
	private boolean withLinkingCheck;

	private String processDqSystem;
	private String exchangeDqSystem;
	private AggregationType dqAggregation;
	private NAHandling dqNaHandling;
	private boolean dqCeiling;

	static void save(Setup setup, IDatabase db) {
		if (setup == null || setup.calcSetup == null)
			return;

		var prefs = new CalculationPreferences();
		prefs.type = setup.type;
		var cs = setup.calcSetup;
		prefs.allocation = cs.allocation();
		prefs.withCosts = cs.hasCosts();
		prefs.withRegionalization = cs.hasRegionalization();
		if (cs.impactMethod() != null) {
			prefs.impactMethod = cs.impactMethod().refId;
		}
		if (cs.nwSet() != null) {
			prefs.nwSet = cs.nwSet().refId;
		}
		cs.simulationRuns().ifPresent(i -> prefs.simulationRuns = i);

		// data quality
		if (setup.withDataQuality && setup.dqSetup != null) {
			prefs.withDataQuality = true;
			var dqs = setup.dqSetup;
			prefs.dqCeiling = dqs.ceiling;
			prefs.dqAggregation = dqs.aggregationType;
			prefs.dqNaHandling = dqs.naHandling;
			if (dqs.processSystem != null) {
				prefs.processDqSystem = dqs.processSystem.refId;
			}
			if (dqs.exchangeSystem != null) {
				prefs.exchangeDqSystem = dqs.exchangeSystem.refId;
			}
		}

		try {
			// save as database (global) preferences
			var dbDir = DatabaseDir.getFileStorageLocation(db);
			Dirs.createIfAbsent(dbDir);
			var dbFile = new File(dbDir, FILE);
			Json.write(prefs.toGlobalJson(), dbFile);

			// save as system/process preferences
			var sysDir = DatabaseDir.getDir(cs.target());
			Dirs.createIfAbsent(sysDir);
			var sysFile = new File(sysDir, FILE);
			Json.write(prefs.toSystemJson(), sysFile);

		} catch (Exception e) {
			ErrorReporter.on("failed to save calculation preferences", e);
		}
	}

	static void apply(Setup setup, IDatabase db) {
		if (setup == null || setup.calcSetup == null || db == null)
			return;

		applyDefaults(setup);

		// load preferences from file
		var cs = setup.calcSetup;
		CalculationPreferences prefs;
		try {
			JsonObject json = null;
			// check if there is a global file
			var sysDir = DatabaseDir.getDir(cs.target());
			var sysFile = new File(sysDir, FILE);
			if (sysFile.exists()) {
				json = Json.readObject(sysFile).orElse(null);
			}
			// check if there is a local file
			if (json == null) {
				var dbDir = DatabaseDir.getFileStorageLocation(db);
				var dbFile = new File(dbDir, FILE);
				if (dbFile.exists()) {
					json = Json.readObject(dbFile).orElse(null);
				}
			}
			prefs = json != null
					? fromJson(json)
					: null;
		} catch (Exception e) {
			ErrorReporter.on("failed to load calculation preferences", e);
			return;
		}

		if (prefs == null)
			return;

		setImpactMethod(db, cs, prefs);
		if (prefs.allocation != null) {
			cs.withAllocation(prefs.allocation);
		}
		cs.withRegionalization(prefs.withRegionalization);
		cs.withCosts(prefs.withCosts);

		// data quality settings
		setup.withDataQuality = prefs.withDataQuality;
		if (setup.dqSetup != null) {
			var dqs = setup.dqSetup;
			dqs.ceiling = prefs.dqCeiling;
			if (prefs.dqNaHandling != null) {
				dqs.naHandling = prefs.dqNaHandling;
			}
			if (prefs.dqAggregation != null) {
				dqs.aggregationType = prefs.dqAggregation;
			}
			dqs.processSystem = prefs.processDqSystem != null
					? db.get(DQSystem.class, prefs.processDqSystem)
					: null;
			dqs.exchangeSystem = prefs.exchangeDqSystem != null
					? db.get(DQSystem.class, prefs.exchangeDqSystem)
					: null;
		}
	}

	private static void setImpactMethod(
			IDatabase db, CalculationSetup cs, CalculationPreferences prefs
	) {
		if (prefs.impactMethod == null)
			return;
		var method = db.get(ImpactMethod.class, prefs.impactMethod);
		if (method == null)
			return;
		cs.withImpactMethod(method);
		if (prefs.nwSet == null)
			return;
		method.nwSets.stream()
				.filter(nw -> prefs.nwSet.equals(nw.refId))
				.findAny()
				.ifPresent(cs::withNwSet);
	}

	static void applyDefaults(Setup setup) {
		setup.type = CalculationType.LAZY;
		if (setup.calcSetup != null) {
			var cs = setup.calcSetup;
			cs.withAllocation(AllocationMethod.USE_DEFAULT)
					.withCosts(false)
					.withRegionalization(false)
					.withSimulationRuns(100);
		}
		setup.withDataQuality = false;
		if (setup.dqSetup != null) {
			var dqs = setup.dqSetup;
			dqs.aggregationType = AggregationType.WEIGHTED_AVERAGE;
			dqs.naHandling = NAHandling.EXCLUDE;
			dqs.ceiling = false;
			if (setup.calcSetup != null) {
				var p = setup.calcSetup.process();
				if (p != null) {
					dqs.processSystem = p.dqSystem;
					dqs.exchangeSystem = p.exchangeDqSystem;
				}
			}
		}
	}

	private JsonObject toGlobalJson() {
		var json = new JsonObject();
		Json.put(json, "impactMethod", impactMethod);
		Json.put(json, "nwSet", nwSet);
		Json.put(json, "simulationRuns", simulationRuns);
		Json.put(json, "processDqSystem", processDqSystem);
		Json.put(json, "exchangeDqSystem", exchangeDqSystem);
		Json.put(json, "dqAggregation", dqAggregation);
		Json.put(json, "dqNaHandling", dqNaHandling);
		Json.put(json, "dqCeiling", dqCeiling);
		Json.put(json, "withLinkingCheck", withLinkingCheck);
		return json;
	}

	private JsonObject toSystemJson() {
		var json = toGlobalJson();
		Json.put(json, "type", type);
		Json.put(json, "allocation", allocation);
		Json.put(json, "withRegionalization", withRegionalization);
		Json.put(json, "withCosts", withCosts);
		Json.put(json, "withDataQuality", withDataQuality);
		return json;
	}

	private static CalculationPreferences fromJson(JsonObject obj) {
		var prefs = new CalculationPreferences();
		prefs.type = Json.getEnum(obj, "type", CalculationType.class);
		prefs.allocation = Json.getEnum(obj, "allocation", AllocationMethod.class);
		prefs.impactMethod = Json.getString(obj, "impactMethod");
		prefs.nwSet = Json.getString(obj, "nwSet");
		Json.getInt(obj, "simulationRuns").ifPresent(i -> prefs.simulationRuns = i);
		prefs.withRegionalization = Json.getBool(obj, "withRegionalization", false);
		prefs.withCosts = Json.getBool(obj, "withCosts", false);
		prefs.withLinkingCheck = Json.getBool(obj, "withLinkingCheck", false);

		prefs.withDataQuality = Json.getBool(obj, "withDataQuality", false);
		prefs.processDqSystem = Json.getString(obj, "processDqSystem");
		prefs.exchangeDqSystem = Json.getString(obj, "exchangeDqSystem");
		prefs.dqAggregation = Json.getEnum(obj, "dqAggregation", AggregationType.class);
		prefs.dqNaHandling = Json.getEnum(obj, "dqNaHandling", NAHandling.class);
		prefs.dqCeiling = Json.getBool(obj, "dqCeiling", false);
		return prefs;
	}
}
