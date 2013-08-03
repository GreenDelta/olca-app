package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.FlowType;

public class FlowTypeViewer extends AbstractComboViewer<FlowType> {

	public FlowTypeViewer(Composite parent) {
		super(parent);
		setInput(FlowType.values());
	}

	@Override
	public Class<FlowType> getType() {
		return FlowType.class;
	}

}
