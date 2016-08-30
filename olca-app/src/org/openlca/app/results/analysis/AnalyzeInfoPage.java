package org.openlca.app.results.analysis;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
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
import org.openlca.core.results.FullResultProvider;

/**
 * Overall information page of the analysis editor.
 */
public class AnalyzeInfoPage extends FormPage {

	private CalculationSetup setup;
	private FullResultProvider result;
	private DQResult dqResult;
	private FormToolkit tk;

	public AnalyzeInfoPage(FormEditor editor, FullResultProvider result, DQResult dqResult, CalculationSetup setup) {
		super(editor, "AnalyzeInfoPage", M.GeneralInformation);
		this.setup = setup;
		this.result = result;
		this.dqResult = dqResult;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.AnalysisResultOf + " "
				+ Labels.getDisplayName(setup.productSystem));
		tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);
		createInfoSection(body);
		resultSections(body);
		if (dqResult != null) {
			new DQInfoSection(body, tk, result, dqResult);
		}
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite comp = UI.formSection(body, tk, M.GeneralInformation);
		ProductSystem system = setup.productSystem;
		text(comp, M.ProductSystem, system.getName());
		String refAmount = setup.getAmount() + " "
				+ setup.getUnit().getName() + " "
				+ system.getReferenceExchange().getFlow().getName();
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

	private void exportButton(Composite comp) {
		tk.createLabel(comp, "");
		Button button = tk.createButton(comp, M.ExportToExcel, SWT.NONE);
		button.setImage(Images.get(FileType.EXCEL));
		Controls.onSelect(button, (e) -> new ExcelExportAction("Analysis result").run());
	}

	private void text(Composite comp, String label, String val) {
		Text text = UI.formText(comp, tk, label);
		text.setText(val);
		text.setEditable(false);
	}

	private void resultSections(Composite body) {
		ContributionChartSection.forFlows(result).render(body, tk);
		if (result.hasImpactResults()) {
			ContributionChartSection.forImpacts(result).render(body, tk);
		}
	}
}
