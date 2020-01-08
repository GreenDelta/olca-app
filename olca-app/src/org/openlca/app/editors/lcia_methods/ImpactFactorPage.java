package org.openlca.app.editors.lcia_methods;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;

class ImpactFactorPage extends ModelPage<ImpactCategory> {

	private ImpactCategoryEditor editor;
	private ImpactFactorTable factorTable;
	private ScrolledForm form;

	ImpactFactorPage(ImpactCategoryEditor editor) {
		super(editor, "ImpactFactorsPage", M.ImpactFactors);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = UI.formHeader(this);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		Section section = UI.section(body, tk, M.ImpactFactors);
		UI.gridData(section, true, true);
		Composite client = tk.createComposite(section);
		section.setClient(client);
		UI.gridLayout(client, 1);
		factorTable = new ImpactFactorTable(editor);
		factorTable.render(client, section);
		form.reflow(true);
	}

}
