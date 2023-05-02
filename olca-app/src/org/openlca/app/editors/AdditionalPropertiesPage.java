package org.openlca.app.editors;

import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.trees.Trees;
import org.openlca.core.model.RootEntity;

public class AdditionalPropertiesPage<T extends RootEntity> extends ModelPage<T> {

	public AdditionalPropertiesPage(ModelEditor<T> editor) {
		super(editor, "AdditionalPropertiesPage", "Additional properties");
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(this);
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		var tree = Trees.createViewer(body, "Key", "Value");
		UI.gridData(tree.getTree(), true, true);
	}
}
