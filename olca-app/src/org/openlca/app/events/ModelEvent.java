package org.openlca.app.events;

import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptors;

public class ModelEvent {

	private CategorizedDescriptor model;
	private Type type;

	public ModelEvent(CategorizedDescriptor model, Type type) {
		this.model = model;
		this.type = type;
	}

	public ModelEvent(CategorizedEntity model, Type type) {
		this.model = Descriptors.toDescriptor(model);
		this.type = type;
	}

	public CategorizedDescriptor getModel() {
		return model;
	}

	public Type getType() {
		return type;
	}

	public static enum Type {
		CREATE, DELETE, MODIFY;
	}

}
