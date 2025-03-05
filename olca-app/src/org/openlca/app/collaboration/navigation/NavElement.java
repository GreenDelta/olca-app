package org.openlca.app.collaboration.navigation;
	
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Libraries;
import org.openlca.core.model.TypedRefId;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;

record NavElement(ElementType type, Object content, boolean isFromLibrary, List<NavElement> children) {

	NavElement(ElementType type, Object content) {
		this(type, content, false, new ArrayList<>());
	}

	NavElement(ElementType type) {
		this(type, null, false, new ArrayList<>());
	}

	NavElement(Descriptor d) {
		this(ElementType.MODEL, d, Libraries.isFrom(d), new ArrayList<>());
	}

	boolean is(ElementType... types) {
		if (types == null)
			return false;
		for (var type : types)
			if (type() == type)
				return true;
		return false;
	}

	TypedRefId getTypedRefId() {
		if (!is(ElementType.MODEL))
			return null;
		var d = (RootDescriptor) content();
		return new TypedRefId(d.type, d.refId);
	}
	
	enum ElementType {

		DATABASE, DATAPACKAGES, DATAPACKAGE, GROUP, MODEL_TYPE, CATEGORY, MODEL;

	}

}