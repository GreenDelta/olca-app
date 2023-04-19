package org.openlca.app.collaboration.navigation;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Cache;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
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
			return index.getPath(Cache.getPathCache(), (RootDescriptor) content());
		return null;
	}

	enum ElementType {

		DATABASE, GROUP, MODEL_TYPE, CATEGORY, MODEL;

		static ElementType get(INavigationElement<?> elem) {
			if (elem instanceof DatabaseElement)
				return DATABASE;
			if (elem instanceof GroupElement)
				return GROUP;
			if (elem instanceof ModelTypeElement)
				return MODEL_TYPE;
			if (elem instanceof CategoryElement)
				return CATEGORY;
			if (elem instanceof ModelElement)
				return MODEL;
			return null;
		}

	}

}