package org.openlca.app.editors.flow_properties;

import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.FlowProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Form editor for editing flow properties
 */
public class FlowPropertyEditor extends ModelEditor<FlowProperty> {

	public static String ID = "editors.flowproperty";
	private Logger log = LoggerFactory.getLogger(getClass());

	public FlowPropertyEditor() {
		super(FlowProperty.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new FlowPropertyInfoPage(this));
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

}
