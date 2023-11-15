package org.openlca.app.collaboration.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.RootDescriptor;
import org.openlca.git.GitIndex;

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

	String getPath(GitIndex index) {
		if (is(ElementType.MODEL_TYPE))
			return index.getPath((ModelType) content());
		if (is(ElementType.CATEGORY))
			return index.getPath((Category) content());
		if (is(ElementType.MODEL))
			return index.getPath(NavRoot.get().categoryPaths, (RootDescriptor) content());
		return null;
	}

	enum ElementType {

		DATABASE, LIBRARY_DIR, LIBRARY, GROUP, MODEL_TYPE, CATEGORY, MODEL;

	}

}