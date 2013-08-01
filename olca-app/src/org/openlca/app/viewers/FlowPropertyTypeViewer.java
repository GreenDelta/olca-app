package org.openlca.app.viewers;

import org.eclipse.swt.widgets.Composite;
import org.openlca.core.model.FlowPropertyType;

public class FlowPropertyTypeViewer extends
		AbstractComboViewer<FlowPropertyType> {

	public FlowPropertyTypeViewer(Composite parent) {
		super(parent);
		setInput(FlowPropertyType.values());
	}

}
