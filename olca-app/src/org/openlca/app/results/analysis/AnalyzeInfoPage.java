package org.openlca.app.results.analysis;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.core.results.FullResultProvider;

/**
 * Overall information page of the analysis editor.
 */
public class AnalyzeInfoPage extends FormPage {

	private CalculationSetup setup;
	private FullResultProvider result;
	private FormToolkit toolkit;

	public AnalyzeInfoPage(FormEditor editor, FullResultProvider result,
			CalculationSetup setup) {
		super(editor, "AnalyzeInfoPage", M.GeneralInformation);
		this.setup = setup;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText(M.AnalysisResultOf + " "
				+ Labels.getDisplayName(setup.productSystem));
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		createResultSections(body);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				M.GeneralInformation);
		ProductSystem system = setup.productSystem;
		createText(composite, M.ProductSystem, system.getName());
		String targetText = system.getTargetAmount() + " "
				+ system.getTargetUnit().getName() + " "
				+ system.getReferenceExchange().getFlow().getName();
		createText(composite, M.TargetAmount, targetText);
		ImpactMethodDescriptor method = setup.impactMethod;
		if (method != null)
			createText(composite, M.ImpactAssessmentMethod,
					method.getName());
		NwSetDescriptor nwSet = setup.nwSet;
		if (nwSet != null)
			createText(composite, M.NormalizationAndWeightingSet,
					nwSet.getName());
		createExportButton(composite);
	}

	private void createExportButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, M.ExportToExcel,
				SWT.NONE);
		button.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(button, (e) -> new ExcelExport().run());
	}

	private void createText(Composite parent, String label, String val) {
		Text text = UI.formText(parent, toolkit, label);
		text.setText(val);
		text.setEditable(false);
	}

	private void createResultSections(Composite body) {
		ContributionChartSection.forFlows(result).render(body, toolkit);
		if (result.hasImpactResults())
			ContributionChartSection.forImpacts(result).render(body, toolkit);

	}
}
