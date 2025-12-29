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

record PubChemInfo(
	Flow flow, PugCompound compound, PugView view) {

	boolean applyMolecularFormula(Consumer<String> fn) {
		var f = compound.molecularFormula();
		if (Strings.isBlank(f))
			return false;
		if (Strings.isNotBlank(flow.formula)
			&& flow.formula.length() >= f.length())
			return false;
		flow.formula = f;
		fn.accept(f);
		return true;
	}

	boolean applyCasNumber(Consumer<String> fn) {
		var cas = view.cas();
		if (Strings.isBlank(cas))
			return false;
		if (Strings.isNotBlank(flow.casNumber)
			&& flow.casNumber.length() >= cas.length())
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
		MsgBox.error("PubChem request failed", message);
		return Optional.empty();
	}

}
