package org.openlca.app.results;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;

/**
 * Overall information page of the analysis editor.
 */
public class InfoPage extends FormPage {

	private final ResultEditor editor;

	public InfoPage(ResultEditor editor) {
		super(editor, "AnalyzeInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var result = editor.result;
		var form = UI.formHeader(mForm,
				Labels.name(editor.setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mForm.getToolkit();
		var body = UI.formBody(form, tk);
		InfoSection.create(body, tk, editor.setup);
		if (editor.dqResult != null) {
			new DQInfoSection(body, tk, editor);
		}
		if (result.hasImpacts()) {
			ContributionChartSection.forImpacts(editor).render(body, tk);
		}
		if (result.hasEnviFlows()) {
			ContributionChartSection.forFlows(editor).render(body, tk);
		}
		form.reflow(true);
	}
}
