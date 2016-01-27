package org.openlca.app.navigation;

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

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof Group))
			return false;
		Group other = (Group) obj;
		return Strings.nullOrEqual(this.label, other.label);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(label);
	}
}
