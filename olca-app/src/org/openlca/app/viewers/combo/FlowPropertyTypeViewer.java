package org.openlca.app.viewers.combo;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.FlowPropertyType;

public class FlowPropertyTypeViewer extends
		AbstractComboViewer<FlowPropertyType> {

	public FlowPropertyTypeViewer(Composite parent) {
		super(parent);
		setInput(FlowPropertyType.values());
	}
	
	@Override
	public Class<FlowPropertyType> getType() {
		return FlowPropertyType.class;
	}

}
