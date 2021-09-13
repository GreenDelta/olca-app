package org.openlca.app.results;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.results.ContributionResult;

/**
 * Overall information page of the analysis editor.
 */
public class InfoPage extends FormPage {

	private final ResultEditor<?> editor;

	public InfoPage(ResultEditor<?> editor) {
		super(editor, "AnalyzeInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ContributionResult result = editor.result;
		ScrolledForm form = UI.formHeader(mform,
				Labels.name(editor.setup.target()),
				Images.get(editor.result));
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		InfoSection.create(body, tk, editor.setup);
		if (editor.dqResult != null) {
			new DQInfoSection(body, tk, result, editor.dqResult);
		}
		if (result.hasImpacts()) {
			ContributionChartSection.forImpacts(editor).render(body, tk);
		}
		ContributionChartSection.forFlows(editor).render(body, tk);
		form.reflow(true);
	}
}
