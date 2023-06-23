package org.openlca.app.editors.flows;

import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.util.UI;
import org.openlca.core.model.Flow;
import org.openlca.util.Strings;

class FlowPropertiesPage extends ModelPage<Flow> {

	private final FlowEditor editor;

	FlowPropertiesPage(FlowEditor editor) {
		super(editor, "FlowPropertiesPage", M.FlowProperties);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		var form = UI.header(this);
		var tk = managedForm.getToolkit();
		var body = UI.body(form, tk);
		var section = UI.section(body, tk, M.FlowProperties);
		UI.gridData(section, true, true);
		var comp = UI.sectionClient(section, tk, 1);
		var viewer = new FlowPropertyFactorViewer(
				comp, Cache.getEntityCache(), editor);
		setInitialInput(viewer);
		CommentAction.bindTo(
				section, viewer, "flowProperties", editor.getComments());
		editor.onSaved(() -> viewer.setInput(getModel()));
		body.setFocus();
		form.reflow(true);
	}

	private void setInitialInput(FlowPropertyFactorViewer viewer) {
		var factors = getModel().flowPropertyFactors;
		factors.sort((f1, f2) -> {
			var prop1 = f1.flowProperty;
			var prop2 = f2.flowProperty;
			return prop1 != null && prop2 != null
					? Strings.compare(prop1.name, prop2.name)
					: 0;
		});
		viewer.setInput(factors);
	}

}
