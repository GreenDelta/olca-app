package org.openlca.app.editors.systems;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;

public class ProductSystemParameterPage extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ParameterRedefTable table;
	private ScrolledForm form;

	public ProductSystemParameterPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemParameterPage", M.Parameters);
		this.editor = editor;
		editor.onSaved(() -> refreshBindings());
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.Parameters);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit, 1);
		table = new ParameterRedefTable(editor);
		table.create(toolkit, composite);
		table.bindActions(section);
	}

	void refreshBindings() {
		if (table != null)
			table.setInput(editor.getModel().parameterRedefs);
	}

}
