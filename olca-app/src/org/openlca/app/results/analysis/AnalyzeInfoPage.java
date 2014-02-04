package org.openlca.app.results.analysis;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.Messages;
import org.openlca.app.resources.ImageType;
import org.openlca.app.results.ContributionChartSection;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.core.results.FullResultProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overall information page of the analysis editor.
 */
public class AnalyzeInfoPage extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private CalculationSetup setup;
	private FullResultProvider result;
	private FormToolkit toolkit;

	public AnalyzeInfoPage(AnalyzeEditor editor, FullResultProvider result,
			CalculationSetup setup) {
		super(editor, "AnalyzeInfoPage", Messages.GeneralInformation);
		this.setup = setup;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText(Messages.ResultOf + " "
				+ Labels.getDisplayName(setup.getProductSystem()));
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		createResultSections(body);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		ProductSystem system = setup.getProductSystem();
		createText(composite, Messages.ProductSystem, system.getName());
		String targetText = system.getTargetAmount() + " "
				+ system.getTargetUnit().getName() + " "
				+ system.getReferenceExchange().getFlow().getName();
		createText(composite, Messages.TargetAmount, targetText);
		ImpactMethodDescriptor method = setup.getImpactMethod();
		if (method != null)
			createText(composite, Messages.ImpactMethodTitle, method.getName());
		NwSetDescriptor nwSet = setup.getNwSet();
		if (nwSet != null)
			createText(composite, Messages.NormalizationWeightingSet,
					nwSet.getName());
		createExportButton(composite);
	}

	private void createExportButton(Composite composite) {
		toolkit.createLabel(composite, "");
		Button button = toolkit.createButton(composite, Messages.ExportToExcel,
				SWT.NONE);
		button.setImage(ImageType.EXCEL_ICON.get());
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tryExport();
			}
		});
	}

	private void tryExport() {
		// TODO: result export
		// final File exportFile = FileChooser.forExport("*.xlsx",
		// "analysis_result.xlsx");
		// if (exportFile == null)
		// return;
		// final boolean[] success = { false };
		// App.run("Export...", new Runnable() {
		// @Override
		// public void run() {
		// try {
		// new AnalysisResultExport(setup.getProductSystem(),
		// exportFile, Cache.getEntityCache()).run(result);
		// success[0] = true;
		// } catch (Exception exc) {
		// log.error("Excel export failed", exc);
		// }
		// }
		// }, new Runnable() {
		// @Override
		// public void run() {
		// if (success[0]) {
		// InformationPopup.show("Export done");
		// }
		// }
		// });
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
