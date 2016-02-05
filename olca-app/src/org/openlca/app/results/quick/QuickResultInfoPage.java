package org.openlca.app.results.quick;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
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

public class QuickResultInfoPage extends FormPage {

	private QuickResultEditor editor;
	private FormToolkit toolkit;

	public QuickResultInfoPage(QuickResultEditor editor) {
		super(editor, "QuickResultInfoPage", M.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, M.ResultsOf + " "
				+ Labels.getDisplayName(editor.getSetup().productSystem));
		this.toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		createChartSections(body);
		form.reflow(true);
	}

	private void createChartSections(Composite body) {
		ContributionChartSection.forFlows(editor.getResult()).render(body,
				toolkit);
		if (editor.getResult().hasImpactResults()) {
			ContributionChartSection.forImpacts(editor.getResult()).render(
					body, toolkit);
		}
	}

	private void createInfoSection(Composite body) {
		CalculationSetup setup = editor.getSetup();
		if (setup == null || setup.productSystem == null)
			return;
		ProductSystem system = setup.productSystem;
		Composite composite = UI.formSection(body, toolkit,
				M.GeneralInformation);
		String sysText = Labels.getDisplayName(system);
		createText(composite, M.ProductSystem, sysText);
		String allocText = Labels.getEnumText(setup.allocationMethod);
		createText(composite, M.AllocationMethod, allocText);
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

}
