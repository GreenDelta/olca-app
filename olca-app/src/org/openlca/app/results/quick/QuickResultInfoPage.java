package org.openlca.app.results.quick;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.results.DQInfoSection;
import org.openlca.app.results.InfoSection;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.results.SimpleResultProvider;

class QuickResultInfoPage extends FormPage {

	private QuickResultEditor editor;
	private SimpleResultProvider<?> result;
	private DQResult dqResult;
	private FormToolkit tk;

	public QuickResultInfoPage(QuickResultEditor editor, SimpleResultProvider<?> result, DQResult dqResult) {
		super(editor, "QuickResultInfoPage", M.GeneralInformation);
		this.editor = editor;
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		String label = M.ResultsOf + " " + Labels.getDisplayName(editor.getSetup().productSystem);
		ScrolledForm form = UI.formHeader(mform, label);
		this.tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		InfoSection.create(body, tk, editor.getSetup(), "Quick result");
		chartSections(body);
		if (dqResult != null)
			new DQInfoSection(body, tk, result, dqResult);
		form.reflow(true);
	}

	private void chartSections(Composite body) {
		ContributionChartSection.forFlows(editor.getResult()).render(body, tk);
		if (editor.getResult().hasImpactResults()) {
			ContributionChartSection.forImpacts(editor.getResult()).render(body, tk);
		}
	}

}
