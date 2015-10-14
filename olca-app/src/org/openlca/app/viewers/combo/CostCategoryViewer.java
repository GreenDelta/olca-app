package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.descriptors.CostCategoryDescriptor;

public class CostCategoryViewer extends AbstractComboViewer<CostCategoryDescriptor> {

	public CostCategoryViewer(Composite parent) {
		super(parent);
	}

	@Override
	public Class<CostCategoryDescriptor> getType() {
		return CostCategoryDescriptor.class;
	}

}
