package org.openlca.app.results.comparison;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.editors.reports.ReportViewer;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.comparison.display.ProductComparison;
import org.openlca.app.results.comparison.display.TargetCalculationEnum;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;

/**
 * Overall information page of the analysis editor.
 */
public class ComparisonPage extends FormPage {

	private final FormEditor editor;
	private TargetCalculationEnum target;

	public ComparisonPage(ResultEditor<?> editor) {
		super(editor, "ComparisonDiagram", "Comparison Diagram");
		this.editor = editor;
		target = TargetCalculationEnum.IMPACT;
	}

	public ComparisonPage(ReportViewer editor) {
		super(editor, "ComparisonDiagram", "Comparison Diagram");
		this.editor = editor;
		target = TargetCalculationEnum.PRODUCT;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		if (target == TargetCalculationEnum.IMPACT) {
			var e = (ResultEditor<?>) editor;
			ScrolledForm form = UI.formHeader(mform, Labels.name(e.setup.productSystem), Images.get(e.result));
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);
			InfoSection.create(body, tk, e.setup);
			new ProductComparison(body, editor, target, tk).display();
			form.reflow(true);
		} else {
			var e = (ReportViewer) editor;
			ScrolledForm form = UI.formHeader(mform, "Project : " + e.project.name);
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);
			InfoSection.create(body, tk, e.project);
			new ProductComparison(body, editor, target, tk).display();
			form.reflow(true);
		}
	}
}