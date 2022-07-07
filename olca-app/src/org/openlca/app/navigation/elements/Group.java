package org.openlca.app.navigation.elements;

import java.util.Objects;

import org.openlca.core.model.ModelType;
import org.openlca.util.Strings;

public class Group {

	public final String label;
	public final GroupType type;
	public final ModelType[] types;

	Group(String label, GroupType type, ModelType... types) {
		this.label = label;
		this.type = type;
		this.types = types;
	}

	static Group of(String label, GroupType type, ModelType... types) {
		return new Group(label, type, types);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Group other))
			return false;
		return Strings.nullOrEqual(this.label, other.label);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(label);
	}
}
