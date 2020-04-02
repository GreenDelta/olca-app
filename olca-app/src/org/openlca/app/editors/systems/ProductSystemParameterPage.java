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

/**
 * @deprecated this is replaced by the new parameter page.
 */
@Deprecated
public class ProductSystemParameterPage extends ModelPage<ProductSystem> {

	private ProductSystemEditor editor;
	private ParameterRedefTable table;
	private ScrolledForm form;

	public ProductSystemParameterPage(ProductSystemEditor editor) {
		super(editor, "ProductSystemParameterPage", M.Parameters);
		this.editor = editor;
		editor.onSaved(() -> {
			if (table != null) {
				table.update();
			}
		});
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = UI.formHeader(this);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		Section section = UI.section(body, toolkit, M.Parameters);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit, 1);
		table = new ParameterRedefTable(
				editor, () -> editor.getModel().parameterRedefs);
		table.create(toolkit, composite);
		table.bindActions(section);
	}

}
