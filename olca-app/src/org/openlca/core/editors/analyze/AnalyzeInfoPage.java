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
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.editors.AnalyzeEditorInput;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Colors;
import org.openlca.app.util.InformationPopup;
import org.openlca.app.util.UI;
import org.openlca.core.editors.io.ui.FileChooser;
import org.openlca.core.model.Flow;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.AnalysisResult;
import org.openlca.io.xls.results.AnalysisResultExport;
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
		super(editor, "AnalyzeInfoPage", Messages.GeneralInformation);
		this.input = editorInput;
		this.result = result;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		toolkit = managedForm.getToolkit();
		toolkit.getHyperlinkGroup().setHyperlinkUnderlineMode(
				HyperlinkSettings.UNDERLINE_HOVER);
		form.setText(Messages.ResultOf + " "); // TODO: product system name
		// + result.getSetup().getProductSystem().getName());
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createInfoSection(body);
		createExportSection(body);
		createResultSections(body);
		form.reflow(true);
	}

	private void createInfoSection(Composite body) {
		Composite composite = UI.formSection(body, toolkit,
				Messages.GeneralInformation);
		// TODO: system infos
		// ProductSystem system = result.getSetup().getProductSystem();
		createText(composite, Messages.ProductSystem, ""); // system.getName());
		// String targetText = system.getTargetAmount() + " "
		// + system.getTargetUnit().getName() + " "
		// + system.getReferenceExchange().getFlow().getName();
		createText(composite, Messages.TargetAmount, ""); // targetText);
		ImpactMethodDescriptor method = input.getMethodDescriptor();
		if (method != null)
			createText(composite, Messages.ImpactMethodTitle, method.getName());
		NormalizationWeightingSet nwSet = input.getNwSet();
		if (nwSet != null)
			createText(composite, Messages.NormalizationWeightingSet,
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
					new AnalysisResultExport(exportFile, )
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
		flowSection.setSectionTitle(Messages.FlowContributions);
		flowSection.setSelectionName(Messages.Flow);
		flowSection.render(body, toolkit);
		if (result.hasImpactResults()) {
			ImpactContributionProvider impactProvider = new ImpactContributionProvider(
					result);
			ChartSection<ImpactCategoryDescriptor> impactSection = new ChartSection<>(
					impactProvider);
			impactSection.setSectionTitle(Messages.ImpactContributions);
			impactSection.setSelectionName(Messages.ImpactCategory);
			impactSection.render(body, toolkit);
		}
	}

}
