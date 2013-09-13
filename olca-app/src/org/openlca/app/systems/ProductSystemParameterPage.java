package org.openlca.app.systems;

import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.ParameterRedefTable;
import org.openlca.app.util.UI;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;

public class ProductSystemParameterPage extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ProductSystem system;
	private List<ParameterRedef> redefinitions;

	public ProductSystemParameterPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemParameterPage", Messages.Parameters);
		this.system = editor.getModel();
		this.redefinitions = this.system.getParameterRedefs();
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ProductSystem
				+ ": " + system.getName());
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, Messages.Parameters);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit);
		ParameterRedefTable table = new ParameterRedefTable(editor,
				redefinitions);
		table.create(toolkit, composite);
		table.bindActions(section);
	}

}
