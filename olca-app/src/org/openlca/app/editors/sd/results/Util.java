package org.openlca.app.editors.sd.results;

import java.util.List;

import org.openlca.sd.eqn.Cell.NumCell;
import org.openlca.sd.eqn.Var;
import org.openlca.util.Strings;

class Util {

	private Util() {
	}

	static List<Var> numericVarsOf(List<Var> vars) {
		if (vars == null || vars.isEmpty())
			return List.of();
		return vars.stream()
				.filter(v -> v.value() instanceof NumCell)
				.sorted((vi, vj) -> {
					var ni = vi.name().label();
					var nj = vj.name().label();
					return Strings.compare(ni, nj);
				})
				.toList();
	}
}
