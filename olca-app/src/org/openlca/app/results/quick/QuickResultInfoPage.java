package org.openlca.app.results.quick;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.results.contributions.ContributionChartSection;
import org.openlca.app.util.Controls;
import org.openlca.app.util.InformationPopup;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.io.xls.results.QuickResultExport;

public class QuickResultInfoPage extends FormPage {

	private QuickResultEditor editor;
	private FormToolkit toolkit;

	public QuickResultInfoPage(QuickResultEditor editor) {
		super(editor, "QuickResultInfoPage", Messages.GeneralInformation);
		this.editor = editor;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.ResultsOf + " "
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
				Messages.GeneralInformation);
		String sysText = Labels.getDisplayName(system);
		createText(composite, Messages.ProductSystem, sysText);
		String allocText = Labels.getEnumText(setup.allocationMethod);
		createText(composite, Messages.AllocationMethod, allocText);
		String targetText = system.getTargetAmount() + " "
				+ system.getTargetUnit().getName() + " "
				+ system.getReferenceExchange().getFlow().getName();
		createText(composite, Messages.TargetAmount, targetText);
		ImpactMethodDescriptor method = setup.impactMethod;
		if (method != null)
			createText(composite, Messages.ImpactAssessmentMethod,
					method.getName());
		NwSetDescriptor nwSet = setup.nwSet;
		if (nwSet != null)
			createText(composite, Messages.NormalizationAndWeightingSet,
					nwSet.getName());
		createExportButton(composite);
	}

	private void createExportButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, Messages.ExportToExcel,
				SWT.NONE);
		button.setImage(ImageType.FILE_EXCEL.get());
		Controls.onSelect(button, (e) -> tryExport());
	}

	private void tryExport() {
		final File exportFile = FileChooser.forExport("*.xlsx",
				"quick_result.xlsx");
		if (exportFile == null)
			return;
		QuickResultExport export = new QuickResultExport(
				editor.getSetup(), editor.getResult(), Cache.getEntityCache());
		export.setExportFile(exportFile);
		App.run(Messages.Export, export, new Runnable() {
			@Override
			public void run() {
				if (export.doneWithSuccess()) {
					InformationPopup.show(Messages.ExportDone);
				}
			}
		});
	}

	private void createText(Composite parent, String label, String val) {
		Text text = UI.formText(parent, toolkit, label);
		text.setText(val);
		text.setEditable(false);
	}

}
