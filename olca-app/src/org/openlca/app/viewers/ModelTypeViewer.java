package org.openlca.app.viewers;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.ModelType;

public class ModelTypeViewer extends AbstractComboViewer<ModelType> {

	protected ModelTypeViewer(Composite parent) {
		super(parent);
		setInput(ModelType.values());
	}

	@Override
	public Class<ModelType> getType() {
		return ModelType.class;
	}
}
