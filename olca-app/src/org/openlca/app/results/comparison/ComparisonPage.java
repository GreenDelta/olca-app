package org.openlca.app.results.comparison;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.comparison.display.Config;
import org.openlca.app.results.comparison.display.ProductComparison;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;

/**
 * Overall information page of the analysis editor.
 */
public class ComparisonPage extends FormPage {

	private final ResultEditor<?> editor;

	public ComparisonPage(ResultEditor<?> editor) {
		super(editor, "ComparisonPage", "Comparison Page");
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, Labels.name(editor.setup.productSystem), Images.get(editor.result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		var config = new Config(); // Comparison config
		InfoSection.create(body, tk, editor.setup);
		new ProductComparison(body, config, editor, tk).display();
		form.reflow(true);
	}
}