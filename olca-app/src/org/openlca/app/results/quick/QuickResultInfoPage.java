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
import org.openlca.app.results.DQInfoSection;
import org.openlca.app.results.ExcelExportAction;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Controls;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.core.results.SimpleResultProvider;

class QuickResultInfoPage extends FormPage {

	private QuickResultEditor editor;
	private SimpleResultProvider<?> result;
	private DQResult dqResult;
	private FormToolkit tk;

	public QuickResultInfoPage(QuickResultEditor editor, SimpleResultProvider<?> result, DQResult dqResult) {
		super(editor, "QuickResultInfoPage", M.GeneralInformation);
		this.editor = editor;
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.ResultsOf + " "
				+ Labels.getDisplayName(editor.getSetup().productSystem));
		this.tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		infoSection(body);
		chartSections(body);
		if (dqResult != null)
			new DQInfoSection(body, tk, result, dqResult);
		form.reflow(true);
	}

	private void chartSections(Composite body) {
		ContributionChartSection.forFlows(editor.getResult()).render(body, tk);
		if (editor.getResult().hasImpactResults()) {
			ContributionChartSection.forImpacts(
					editor.getResult()).render(body, tk);
		}
	}

	private void infoSection(Composite body) {
		CalculationSetup setup = editor.getSetup();
		if (setup == null || setup.productSystem == null)
			return;
		ProductSystem system = setup.productSystem;
		Composite comp = UI.formSection(body, tk, M.GeneralInformation);
		String sysText = Labels.getDisplayName(system);
		text(comp, M.ProductSystem, sysText);
		String allocText = Labels.getEnumText(setup.allocationMethod);
		text(comp, M.AllocationMethod, allocText);
		String refAmount = setup.getAmount() + " " + setup.getUnit().getName()
				+ " " + system.getReferenceExchange().getFlow().getName();
		text(comp, M.TargetAmount, refAmount);
		ImpactMethodDescriptor method = setup.impactMethod;
		if (method != null) {
			text(comp, M.ImpactAssessmentMethod, method.getName());
		}
		NwSetDescriptor nwSet = setup.nwSet;
		if (nwSet != null) {
			text(comp, M.NormalizationAndWeightingSet, nwSet.getName());
		}
		exportButton(comp);
	}

	private void exportButton(Composite composite) {
		tk.createLabel(composite, "");
		Button button = tk.createButton(composite, M.ExportToExcel, SWT.NONE);
		button.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(button, (e) -> new ExcelExportAction("Quick result").run());
	}

	private void text(Composite comp, String label, String val) {
		Text text = UI.formText(comp, tk, label);
		text.setText(val);
		text.setEditable(false);
	}

}
