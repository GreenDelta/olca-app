package org.openlca.app.collaboration.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.Descriptor;

record NavElement(ElementType type, Object content, boolean isFromLibrary, List<NavElement> children) {

	NavElement(ElementType type, Object content, List<NavElement> children) {
		this(type, content, false, children);
	}

	NavElement(ElementType type, Object content) {
		this(type, content, content instanceof Descriptor d && d.isFromLibrary(), new ArrayList<>());
	}

	boolean is(ElementType... types) {
		if (types == null)
			return false;
		for (var type : types)
			if (type() == type)
				return true;
		return false;
	}

	enum ElementType {

		DATABASE, LIBRARY_DIR, LIBRARY, GROUP, MODEL_TYPE, CATEGORY, MODEL;

	}

}