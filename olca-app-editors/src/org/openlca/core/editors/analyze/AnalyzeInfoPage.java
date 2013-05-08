package org.openlca.core.editors.analyze;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.views.AnalyzeEditorInput;
import org.openlca.core.editors.io.AnalysisResultExport;
import org.openlca.core.editors.io.ui.FileChooser;
import org.openlca.core.model.Flow;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.Colors;
import org.openlca.ui.InformationPopup;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overall information page of the analysis editor.
 */
public class AnalyzeInfoPage extends FormPage {

	private Logger log = LoggerFactory.getLogger(getClass());
	private AnalyzeEditorInput input;
	private AnalysisResult result;
	private FormToolkit toolkit;

	public AnalyzeInfoPage(AnalyzeEditor editor, AnalysisResult result,
			AnalyzeEditorInput editorInput) {
		super(editor, "AnalyzeInfoPage", Messages.Common_GeneralInformation);
		this.input = editorInput;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText(Messages.Analyze_ResultOf + " "
				+ result.getSetup().getProductSystem().getName());
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		createExportSection(body);
		createResultSections(body);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.Common_GeneralInformation);
		ProductSystem system = result.getSetup().getProductSystem();
		createText(composite, Messages.Common_ProductSystem, system.getName());
		String targetText = system.getTargetAmount() + " "
				+ system.getTargetUnit().getName() + " "
				+ system.getReferenceExchange().getFlow().getName();
		createText(composite, Messages.Common_TargetAmount, targetText);
		ImpactMethodDescriptor method = input.getMethodDescriptor();
		if (method != null)
			createText(composite, Messages.Common_LCIAMethodTitle,
					method.getName());
		NormalizationWeightingSet nwSet = input.getNwSet();
		if (nwSet != null)
			createText(composite, Messages.Common_NormalizationWeightingSet,
					nwSet.getReferenceSystem());
	}

	private void createExportSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit, "Export");
		ImageHyperlink excelLink = new ImageHyperlink(composite, SWT.NONE);
		excelLink.setImage(ImageType.EXCEL_ICON.get());
		excelLink.setText("Export complete result to MS Excel");
		excelLink.setForeground(Colors.getLinkBlue());
		excelLink.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				tryExport();
			}
		});
	}

	private void tryExport() {
		final File exportFile = FileChooser.forExport("*.xlsx",
				"analysis_result.xlsx");
		if (exportFile == null)
			return;
		final boolean[] success = { false };
		App.run("Export...", new Runnable() {
			@Override
			public void run() {
				try {
					new AnalysisResultExport(exportFile, input.getDatabase())
							.run(result);
					success[0] = true;
				} catch (Exception exc) {
					log.error("Excel export failed", exc);
				}
			}
		}, new Runnable() {
			@Override
			public void run() {
				if (success[0]) {
					InformationPopup.show("Export done");
				}
			}
		});
	}

	private void createText(Composite parent, String label, String val) {
		Text text = UI.formText(parent, toolkit, label);
		text.setText(val);
		text.setEditable(false);
	}

	private void createResultSections(Composite body) {
		FlowContributionProvider flowProvider = new FlowContributionProvider(
				input.getDatabase(), result);
		ChartSection<Flow> flowSection = new ChartSection<>(flowProvider);
		flowSection.setSectionTitle(Messages.Analyze_FlowContributions);
		flowSection.setSelectionName(Messages.Common_Flow);
		flowSection.render(body, toolkit);
		if (result.hasImpactResults()) {
			ImpactContributionProvider impactProvider = new ImpactContributionProvider(
					result);
			ChartSection<ImpactCategoryDescriptor> impactSection = new ChartSection<>(
					impactProvider);
			impactSection.setSectionTitle(Messages.Analyze_ImpactContributions);
			impactSection.setSelectionName(Messages.Common_ImpactCategory);
			impactSection.render(body, toolkit);
		}
	}

}
