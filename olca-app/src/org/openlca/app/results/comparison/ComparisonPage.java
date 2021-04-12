package org.openlca.app.results.comparison;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
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
		InfoSection.create(body, tk, editor.setup);
		var comparison = new ProductComparison(body, editor, tk);
		comparison.run();
//		App.runWithProgress(M.Calculate, comparison, () -> {
//			form.reflow(true);
//		});
		form.reflow(true);

	}
}