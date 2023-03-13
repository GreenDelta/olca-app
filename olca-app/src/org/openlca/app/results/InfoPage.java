package org.openlca.app.results;

import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overall information page of the analysis editor.
 */
public class InfoPage extends FormPage {

	private final ResultEditor editor;
	private final Logger log = LoggerFactory.getLogger(getClass());

	public InfoPage(ResultEditor editor) {
		super(editor, "AnalyzeInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var result = editor.result;
		var form = UI.header(mForm,
				Labels.name(editor.setup.target()),
				Icon.ANALYSIS_RESULT.get());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		InfoSection.create(body, tk, editor.setup);
		if (editor.dqResult != null) {
			new DQInfoSection(body, tk, editor);
		}
		try {
			if (result.hasImpacts()) {
				ContributionChartSection.forImpacts(editor).render(body, tk);
			}
			if (result.hasEnviFlows()) {
				ContributionChartSection.forFlows(editor).render(body, tk);
			}
		} catch (Exception e) {
			log.error("Matrix error: the matrix is probably singular.");
		}
		form.reflow(true);
	}
}
