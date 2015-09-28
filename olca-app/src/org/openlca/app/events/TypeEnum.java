package org.openlca.app.events;

public interface TypeEnum {

	default boolean isOneOf(TypeEnum... types) {
		if (types == null)
			return false;
		for (TypeEnum type : types)
			if (type == this)
				return true;
		return false;
	}

}
