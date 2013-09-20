package org.openlca.app.simulation;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.SimulationResult;
import org.openlca.core.results.SimulationResults;

public class SimulationPage extends FormPage {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = FLOW;

	private EntityCache cache = Cache.getEntityCache();
	private SimulationEditor editor;
	private StatisticsCanvas statisticsCanvas;
	private ProgressBar progressBar;
	private FlowViewer flowViewer;
	private Section progressSection;
	private ScrolledForm form;
	private SimulationResult result;
	private ImpactCategoryViewer impactViewer;

	public SimulationPage(SimulationEditor editor) {
		super(editor, "SimulationPage", "Simulation");
		this.editor = editor;
		this.result = editor.getSimulator().getResult();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(Messages.MonteCarloSimulation);
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createSettingsSection(toolkit, body);
		createProgressSection(toolkit, body);
		createResultSection(toolkit, body);
		form.pack();
	}

	private void createSettingsSection(FormToolkit toolkit, Composite body) {
		Composite settings = UI.formSection(body, toolkit, Messages.Settings);
		Text systemText = UI
				.formText(settings, toolkit, Messages.ProductSystem);
		Text processText = UI.formText(settings, toolkit, Messages.Process);
		Text qRefText = UI.formText(settings, toolkit,
				Messages.QuantitativeReference);
		Text simCountText = UI.formText(settings, toolkit,
				Messages.Simulation_NumberOfSimulations);
		if (editor.getSetup() != null) {
			CalculationSetup setup = editor.getSetup();
			systemText.setText(setup.getProductSystem().getName());
			processText.setText(setup.getProductSystem().getReferenceProcess()
					.getName());
			// qRefText.setText(input.getQuantitativeReference()); TODO
			simCountText.setText(Integer.toString(setup.getNumberOfRuns()));
		}
		systemText.setEditable(false);
		processText.setEditable(false);
		qRefText.setEditable(false);
		simCountText.setEditable(false);
	}

	private void createProgressSection(FormToolkit toolkit, Composite body) {
		progressSection = UI.section(body, toolkit, Messages.Progress);
		Composite composite = UI.sectionClient(progressSection, toolkit);
		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setMaximum(editor.getSetup().getNumberOfRuns());
		UI.gridWidth(progressBar, 470);
		final Button progressButton = toolkit.createButton(composite,
				Messages.Start, SWT.NONE);
		UI.gridWidth(progressButton, 70);
		new SimulationControl(progressButton, editor, this);
	}

	private void createResultSection(FormToolkit toolkit, Composite body) {
		if (result == null)
			return;
		Section section = UI.section(body, toolkit, Messages.Results);
		SimulationExportAction exportAction = new SimulationExportAction();
		exportAction.configure(result);
		Actions.bind(section, exportAction);
		Composite composite = UI.sectionClient(section, toolkit);
		initFlowCheckViewer(toolkit, composite);
		if (result.hasImpactResults())
			initImpactCheckViewer(toolkit, composite);
		statisticsCanvas = new StatisticsCanvas(body);
		UI.gridData(statisticsCanvas, true, true).verticalIndent = 5;
	}

	private void initImpactCheckViewer(FormToolkit toolkit, Composite section) {
		Button impactCheck = toolkit.createButton(section,
				Messages.ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(section);
		impactViewer.setEnabled(false);
		Set<ImpactCategoryDescriptor> impacts = SimulationResults.getImpacts(
				result, cache);
		impactViewer.setInput(impacts);
		impactViewer
				.addSelectionChangedListener(new SelectionChange<ImpactCategoryDescriptor>());
		impactViewer.selectFirst();
		new ResultTypeCheck<>(impactViewer, impactCheck, IMPACT);
	}

	private void initFlowCheckViewer(FormToolkit toolkit, Composite section) {
		Button flowsCheck = toolkit.createButton(section, Messages.Flows,
				SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new FlowViewer(section, cache);
		Set<FlowDescriptor> flows = SimulationResults.getFlows(result, cache);
		flowViewer.setInput(flows.toArray(new FlowDescriptor[flows.size()]));
		flowViewer.selectFirst();
		flowViewer
				.addSelectionChangedListener(new SelectionChange<FlowDescriptor>());
		new ResultTypeCheck<>(flowViewer, flowsCheck, FLOW);
	}

	private void updateSelection() {
		if (result == null || statisticsCanvas == null)
			return;
		if (resultType == FLOW) {
			FlowDescriptor flow = flowViewer.getSelected();
			if (flow != null)
				statisticsCanvas.setValues(result.getFlowResults(flow.getId()));
		} else {
			ImpactCategoryDescriptor cat = impactViewer.getSelected();
			if (cat != null)
				statisticsCanvas
						.setValues(result.getImpactResults(cat.getId()));
		}
	}

	void updateProgress() {
		if (result == null)
			return;
		updateSelection();
		progressBar.setSelection(progressBar.getSelection() + 1);
	}

	void progressDone(int numberOfIteration) {
		// TODO: update count text etc.
		// TODO: dispose the solver!
		progressSection.setExpanded(false);
		progressSection.pack();
		progressSection.setVisible(false);
		form.reflow(true);
	}

	private class SelectionChange<T> implements ISelectionChangedListener<T> {
		@Override
		public void selectionChanged(T selection) {
			updateSelection();
		}
	}

	private class ResultTypeCheck<T> implements SelectionListener {

		private AbstractComboViewer<T> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractComboViewer<T> viewer, Button check,
				int type) {
			this.viewer = viewer;
			this.check = check;
			this.type = type;
			check.addSelectionListener(this);
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			if (check.getSelection()) {
				viewer.setEnabled(true);
				resultType = this.type;
				updateSelection();
			} else
				viewer.setEnabled(false);
		}
	}

}
