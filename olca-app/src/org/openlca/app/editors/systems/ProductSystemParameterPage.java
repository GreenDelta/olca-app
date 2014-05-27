package org.openlca.app.editors.systems;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;

public class ProductSystemParameterPage extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ParameterRedefTable table;

	public ProductSystemParameterPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemParameterPage", Messages.Parameters);
		this.editor = editor;
		editor.onSaved(() -> refreshBindings());
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ProductSystem
				+ ": " + editor.getModel().getName());
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, Messages.Parameters);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit);
		table = new ParameterRedefTable(editor);
		table.create(toolkit, composite);
		table.bindActions(section);
	}

	void refreshBindings() {
		if (table != null)
			table.setInput(editor.getModel().getParameterRedefs());
	}

}
