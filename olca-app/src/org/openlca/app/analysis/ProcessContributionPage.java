package org.openlca.app.analysis;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.util.UI;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.AnalysisResult;

class ProcessContributionPage extends FormPage {

	private AnalyzeEditor editor;
	private AnalysisResult result;

	public ProcessContributionPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, "ProcessContributionPage", Messages.ProcessContributions);
		this.editor = editor;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.ProcessContributions);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		ProcessContributionSection<FlowDescriptor> flowSection = createFlowSection(
				body, toolkit);
		ProcessContributionSection<ImpactCategoryDescriptor> impactSection = null;
		if (result.hasImpactResults())
			impactSection = createImpactSection(body, toolkit);

		form.reflow(true);

		flowSection.refreshValues();
		if (result.hasImpactResults())
			impactSection.refreshValues();
	}

	private ProcessContributionSection<ImpactCategoryDescriptor> createImpactSection(
			Composite body, FormToolkit toolkit) {
		ImpactContributionProvider impactProvider = new ImpactContributionProvider(
				result);
		ProcessContributionSection<ImpactCategoryDescriptor> impactSection = new ProcessContributionSection<>(
				impactProvider);
		impactSection.setSectionTitle(Messages.ImpactContributions);
		impactSection.setSelectionName(Messages.ImpactCategory);
		impactSection.render(body, toolkit);
		return impactSection;
	}

	private ProcessContributionSection<FlowDescriptor> createFlowSection(
			Composite body, FormToolkit toolkit) {
		FlowContributionProvider flowProvider = new FlowContributionProvider(
				result);
		ProcessContributionSection<FlowDescriptor> flowSection = new ProcessContributionSection<>(
				flowProvider);
		flowSection.setSectionTitle(Messages.FlowContributions);
		flowSection.setSelectionName(Messages.Flow);
		flowSection.render(body, toolkit);
		return flowSection;
	}
}
