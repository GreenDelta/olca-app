package org.openlca.app.editors.flow_properties;

import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.model.FlowProperty;

/**
 * Form editor for editing flow properties
 */
public class FlowPropertyEditor extends ModelEditor<FlowProperty> {

	public static String ID = "editors.flowproperty";

	public FlowPropertyEditor() {
		super(FlowProperty.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(new FlowPropertyInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}

}
