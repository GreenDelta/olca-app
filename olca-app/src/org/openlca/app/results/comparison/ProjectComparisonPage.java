package org.openlca.app.results.comparison;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.editors.reports.ReportViewer;
import org.openlca.app.results.comparison.display.Config;
import org.openlca.app.results.comparison.display.ProductComparison;
import org.openlca.app.util.UI;

/**
 * Overall information page of the analysis editor.
 */
public class ProjectComparisonPage extends FormPage {

	private final ReportViewer report;

	public ProjectComparisonPage(ReportViewer report) {
		super(report, "ProjectContributionComparison", "Project Contribution comparison");
		this.report = report;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, report.getReport().title, null);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		var config = new Config(); // Comparison config
//		InfoSection.create(body, tk, editor.getReport());
		new ProductComparison(body, config, null,report.getReport(), tk).display();
		form.reflow(true);
	}
}