package org.openlca.app.events;

import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.CategoryDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

public class ModelEvent {

	public final CategorizedDescriptor model;
	public final CategoryDescriptor category;
	public final Type type;

	public ModelEvent(CategorizedDescriptor model, Category category, Type type) {
		this.model = model;
		this.category = Descriptors.toDescriptor(category);
		this.type = type;
	}

	public ModelEvent(CategorizedEntity model, Type type) {
		this(Descriptors.toDescriptor(model), model.getCategory(), type);
	}

	public static enum Type implements TypeEnum {
		CREATE, DELETE, MODIFY;
	}

}
