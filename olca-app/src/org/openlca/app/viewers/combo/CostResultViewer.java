package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.util.CostResultDescriptor;

public class CostResultViewer extends AbstractComboViewer<CostResultDescriptor> {

	public CostResultViewer(Composite parent) {
		super(parent);
	}

	@Override
	public Class<CostResultDescriptor> getType() {
		return CostResultDescriptor.class;
	}

}
