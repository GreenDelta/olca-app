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

	boolean applySmiles(Runnable fn) {
		boolean updated = false;
		var props = flow.readOtherProperties();
		var smiles = compound.connectivitySmiles();
		if (isBetter(smiles, Json.getString(props, "SMILES"))) {
			updated = true;
			Json.put(props, "SMILES", smiles);
		}
		var absoluteSmiles = compound.absoluteSmiles();
		if (isBetter(absoluteSmiles, Json.getString(props, "SMILES - Absolute"))) {
			updated = true;
			Json.put(props, "SMILES - Absolute", absoluteSmiles);
		}
		if (updated) {
			flow.writeOtherProperties(props);
			fn.run();
		}
		return updated;
	}

	private boolean isBetter(String newVal, String oldVal) {
		if (Strings.isBlank(newVal))
			return true;
		return Strings.isBlank(oldVal) || newVal.length() > oldVal.length();
	}

	static Optional<PubChemInfo> getFor(Flow flow) {
		try (var client = new PubChemClient()) {

			var res = client.getCompoundsByName(flow.name);
			if (res.isError())
				return err(res.error());
			var compounds = res.value();
			if (compounds.isEmpty())
				return err("No results for '" + flow.name + "' available.");

			var compound = compounds.getFirst();
			var viewRes = client.getCompoundView(compound.id());
			if (viewRes.isError())
				return err(viewRes.error());
			var view = viewRes.value();
			var info = new PubChemInfo(flow, compound, view);
			return Optional.of(info);
		} catch (Exception e) {
			ErrorReporter.on("PubChem request failed", e);
			return Optional.empty();
		}
	}

	private static Optional<PubChemInfo> err(String message) {
		MsgBox.info("PubChem request failed", message);
		return Optional.empty();
	}

}
