package org.openlca.app.editors.sd.editor.graph.actions.sysdialog;

import org.openlca.commons.Strings;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Var;

import java.util.List;
import java.util.Objects;

class Vars {

	static List<Id> namesOf(SdModel model) {
		if (model == null) return List.of();
		return model.vars()
			.stream()
			.map(Var::name)
			.filter(Objects::nonNull)
			.sorted((i, j) -> Strings.compareIgnoreCase(i.label(), j.label()))
			.toList();
	}

	static boolean contains(SdModel model, Id variable) {
		if (model == null || variable == null) {
			return false;
		}
		for (var v : model.vars()) {
			if (Objects.equals(v.name(), variable)) {
				return true;
			}
		}
		return false;
	}
}
