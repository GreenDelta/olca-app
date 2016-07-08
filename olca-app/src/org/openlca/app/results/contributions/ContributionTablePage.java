package org.openlca.app.results.contributions;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.results.ContributionResultProvider;

public class ContributionTablePage extends FormPage {

	private ContributionResultProvider<?> result;
	private DQResult dqResult;

	public ContributionTablePage(FormEditor editor, ContributionResultProvider<?> result, DQResult dqResult) {
		super(editor, "ProcessContributionPage", M.ProcessContributions);
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, M.ProcessContributions);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createSections(form, toolkit, body);
	}

	private void createSections(ScrolledForm form, FormToolkit toolkit, Composite body) {
		ContributionTableSection flowSection = ContributionTableSection.forFlows(result, dqResult);
		flowSection.render(body, toolkit);
		ContributionTableSection impactSection = null;
		ContributionTableSection costSection = null;
		if (result.hasImpactResults()) {
			impactSection = ContributionTableSection.forImpacts(result, dqResult);
			impactSection.render(body, toolkit);
		}
		if (result.hasCostResults()) {
			costSection = ContributionTableSection.forCosts(result, dqResult);
			costSection.render(body, toolkit);
		}
		form.reflow(true);
		flowSection.refreshValues();
		if (impactSection != null)
			impactSection.refreshValues();
		if (costSection != null)
			costSection.refreshValues();
	}
}
