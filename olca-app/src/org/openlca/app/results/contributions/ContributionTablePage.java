package org.openlca.app.results.contributions;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.core.results.ContributionResultProvider;

public class ContributionTablePage extends FormPage {

	private ContributionResultProvider<?> result;

	public ContributionTablePage(FormEditor editor,
			ContributionResultProvider<?> result) {
		super(editor, "ProcessContributionPage", Messages.ProcessContributions);
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.ProcessContributions);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createSections(form, toolkit, body);
	}

	private void createSections(ScrolledForm form, FormToolkit toolkit,
			Composite body) {
		ContributionTableSection flowSection = ContributionTableSection
				.forFlows(result);
		flowSection.render(body, toolkit);
		ContributionTableSection impactSection = null;
		ContributionTableSection costSection = null;
		if (result.hasImpactResults()) {
			impactSection = ContributionTableSection.forImpacts(result);
			impactSection.render(body, toolkit);
		}
		if (result.hasCostResults()) {
			costSection = ContributionTableSection.forCosts(result);
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
