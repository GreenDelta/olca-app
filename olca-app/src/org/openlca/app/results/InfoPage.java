package org.openlca.app.results;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.results.ContributionResult;

/**
 * Overall information page of the analysis editor.
 */
public class InfoPage extends FormPage {

	private CalculationSetup setup;
	private ContributionResult result;
	private DQResult dqResult;
	private FormToolkit tk;

	public InfoPage(FormEditor editor, ContributionResult result,
			DQResult dqResult, CalculationSetup setup) {
		super(editor, "AnalyzeInfoPage", M.GeneralInformation);
		this.setup = setup;
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform,
				Labels.getDisplayName(setup.productSystem),
				Images.get(result));
		tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		InfoSection.create(body, tk, setup);
		if (result.hasImpactResults()) {
			ContributionChartSection.forImpacts(result).render(body, tk);
		}
		ContributionChartSection.forFlows(result).render(body, tk);
		if (dqResult != null) {
			new DQInfoSection(body, tk, result, dqResult);
		}
		form.reflow(true);
	}

}
