package org.openlca.app.editors.flows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.commons.Strings;
import org.openlca.core.model.Flow;
import org.openlca.io.pubchem.PubChemClient;
import org.openlca.io.pubchem.PugCompound;
import org.openlca.io.pubchem.PugView;
import org.openlca.jsonld.Json;

import com.google.gson.JsonObject;

record PubChemInfo(
	Flow flow, PugCompound compound, PugView view) {

	boolean applyMolecularFormula(Consumer<String> fn) {
		var formula = compound.molecularFormula();
		if (!isBetter(formula, flow.formula))
			return false;
		flow.formula = formula;
		fn.accept(formula);
		return true;
	}

	boolean applyCasNumber(Consumer<String> fn) {
		var cas = view.cas();
		if (!isBetter(cas, flow.casNumber))
			return false;
		flow.casNumber = cas;
		fn.accept(cas);
		return true;
	}

	boolean applySynonyms(Consumer<String> fn) {
		var syns = new ArrayList<String>();
		if (Strings.isNotBlank(flow.synonyms)) {
			for (var syn : flow.synonyms.split(";")) {
				addNewSyn(syn, syns);
			}
		}

		boolean updated = false;
		for (var syn : List.of(
			compound.iupacNamePreferred(),
			compound.iupacNameSystematic(),
			compound.iupacNameTraditional())) {
			if (addNewSyn(syn, syns)) {
				updated = true;
			}
		}
		if (!updated)
			return false;

		Collections.sort(syns);
		var synStr = String.join("; ", syns);
		flow.synonyms = synStr;
		fn.accept(synStr);
		return true;
	}

	private boolean addNewSyn(String s, List<String> syns) {
		if (Strings.isBlank(s))
			return false;
		var syn = s.strip();
		if (Strings.isNotBlank(flow.name)
			&& flow.name.strip().equalsIgnoreCase(syn))
			return false;
		for (var other : syns) {
			if (other.equalsIgnoreCase(syn))
				return false;
		}
		syns.add(syn);
		return true;
	}

	boolean applyProperties(Runnable fn) {
		record Prop(String key, String newVal, String oldVal) {
			static Prop of(String key, String newVal, JsonObject obj) {
				return new Prop(key, newVal, Json.getString(obj, key));
			}
		}

		var json = flow.readOtherProperties();
		var props = List.of(
			Prop.of("Connectivity-SMILES", compound.connectivitySmiles(), json),
			Prop.of("Absolute-SMILES", compound.absoluteSmiles(), json),
			Prop.of("InChI-String", compound.inchiString(), json),
			Prop.of("InChI-Key", compound.inchiKey(), json));

		boolean updated = false;
		for (var prop : props) {
			if (isBetter(prop.newVal, prop.oldVal)) {
				Json.put(json, prop.key, prop.newVal);
				updated = true;
			}
		}
		if (updated) {
			flow.writeOtherProperties(json);
			fn.run();
		}
		return updated;
	}

	private boolean isBetter(String newVal, String oldVal) {
		if (Strings.isBlank(newVal))
			return false;
		return Strings.isBlank(oldVal) || newVal.length() > oldVal.length();
	}

	static Optional<PubChemInfo> getFor(Flow flow) {
		try (var client = new PubChemClient()) {

			var res = client.getCompoundsByName(flow.name);
			if (res.isError())
				return err(flow, res.error());
			var compounds = res.value();
			if (compounds.isEmpty())
				return err(flow, "No results for '" + flow.name + "' available.");

			var compound = compounds.getFirst();
			var viewRes = client.getCompoundView(compound.id());
			if (viewRes.isError())
				return err(flow, viewRes.error());
			var view = viewRes.value();
			var info = new PubChemInfo(flow, compound, view);
			return Optional.of(info);
		} catch (Exception e) {
			ErrorReporter.on("PubChem request failed", e);
			return Optional.empty();
		}
	}

	private static Optional<PubChemInfo> err(Flow flow, String details) {
		MsgBox.info("No compound found", "A compound with the name '"
			+ flow.name + "' could not be found via the PubChem PUG API.\n\n"
			+	"API response details:\n\n" + details);
		return Optional.empty();
	}

}
