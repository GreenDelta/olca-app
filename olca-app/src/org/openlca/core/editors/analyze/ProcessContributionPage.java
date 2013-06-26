package org.openlca.core.editors.analyze;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.ui.UI;

class ProcessContributionPage extends FormPage {

	private AnalyzeEditor editor;
	private AnalysisResult result;

	public ProcessContributionPage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, "ProcessContributionPage",
				Messages.Analyze_ProcessContributions);
		this.editor = editor;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.Analyze_ProcessContributions);
		FormToolkit toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);

		ProcessContributionSection<Flow> flowSection = createFlowSection(body,
				toolkit);
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
		impactSection.setSectionTitle(Messages.Analyze_ImpactContributions);
		impactSection.setSelectionName(Messages.Common_ImpactCategory);
		impactSection.render(body, toolkit);
		return impactSection;
	}

	private ProcessContributionSection<Flow> createFlowSection(Composite body,
			FormToolkit toolkit) {
		FlowContributionProvider flowProvider = new FlowContributionProvider(
				Database.get(), result);
		ProcessContributionSection<Flow> flowSection = new ProcessContributionSection<>(
				flowProvider);
		flowSection.setSectionTitle(Messages.Analyze_FlowContributions);
		flowSection.setSelectionName(Messages.Common_Flow);
		flowSection.render(body, toolkit);
		return flowSection;
	}
}
