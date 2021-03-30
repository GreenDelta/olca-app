package org.openlca.app.editors.parameters.bigtable;

import org.openlca.app.M;
import org.openlca.app.util.Labels;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.util.Strings;

/** Stores a parameter object and its owner. */
class Param implements Comparable<Param> {

	/**
	 * We have the owner ID as a separate field because a parameter could
	 * have a link to an owner that does not exist anymore in the database
	 * (it is an error but such things seem to happen).
	 */
	Long ownerID;

	/** If null, it is a global parameter. */
	CategorizedDescriptor owner;

	Parameter parameter;

	boolean evalError;

	@Override
	public int compareTo(Param other) {
		int c = Strings.compare(
				this.parameter.name,
				other.parameter.name);
		if (c != 0)
			return c;

		if (this.owner == null && other.owner == null)
			return 0;
		if (this.owner == null)
			return -1;
		if (other.owner == null)
			return 1;

		return Strings.compare(
				Labels.name(this.owner),
				Labels.name(other.owner));
	}

	boolean matches(String filter, int type) {
		if (type == FilterCombo.ERRORS)
			return evalError;
		if (parameter == null)
			return false;
		if (filter == null)
			return true;
		String f = filter.trim().toLowerCase();
		if (Strings.nullOrEmpty(f))
			return true;

		if (type == FilterCombo.ALL || type == FilterCombo.NAMES) {
			if (parameter.name != null) {
				String n = parameter.name.toLowerCase();
				if (n.contains(f))
					return true;
			}
		}

		if (type == FilterCombo.ALL || type == FilterCombo.SCOPES) {
			String scope = owner != null
					? Labels.name(owner)
					: M.GlobalParameter;
			scope = scope == null ? "" : scope.toLowerCase();
			if (scope.contains(f))
				return true;
		}

		if (type == FilterCombo.ALL || type == FilterCombo.FORMULAS) {
			if (parameter.formula != null) {
				String formula = parameter.formula.toLowerCase();
				if (formula.contains(f)) {
					return true;
				}
			}
		}

		if (type == FilterCombo.ALL || type == FilterCombo.DESCRIPTIONS) {
			if (parameter.description != null) {
				String d = parameter.description.toLowerCase();
				return d.contains(f);
			}
		}

		return false;
	}

	ParameterScope scope() {
		return parameter.scope == null
				? ParameterScope.GLOBAL
				: parameter.scope;
	}

}