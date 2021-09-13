package org.openlca.app.results.simulation;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.ResultFlowCombo;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.AbstractComboViewer;
import org.openlca.app.viewers.combo.ImpactCategoryViewer;
import org.openlca.core.math.Simulator;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.Flow;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.results.SimulationResult;

class SimulationPage extends FormPage {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = FLOW;

	private final SimulationEditor editor;
	private final Simulator simulator;
	private final SimulationResult result;

	private StatisticsCanvas statisticsCanvas;
	private ProgressBar progressBar;
	private ResultFlowCombo flowViewer;
	private Section progressSection;
	private ScrolledForm form;
	private ImpactCategoryViewer impactViewer;

	/**
	 * A pinned product which results should be displayed.
	 */
	private TechFlow resultPin;

	public SimulationPage(SimulationEditor editor) {
		super(editor, "SimulationPage", M.MonteCarloSimulation);
		this.editor = editor;
		this.simulator = editor.simulator;
		this.result = editor.simulator.getResult();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		form = mform.getForm();
		FormToolkit tk = mform.getToolkit();
		form.setText(M.MonteCarloSimulation);
		tk.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, tk);
		createSettingsSection(tk, body);

		PinBoard pinBoard = new PinBoard(simulator);
		pinBoard.create(tk, body);
		pinBoard.onResultPinChange = (pp) -> {
			this.resultPin = pp;
			updateSelection();
		};

		createProgressSection(tk, body);
		createResultSection(tk, body);
		form.reflow(true);
	}

	private void createSettingsSection(FormToolkit toolkit, Composite body) {
		Composite settings = UI.formSection(body, toolkit, M.Settings);
		Text systemText = UI.formText(settings, toolkit, M.ProductSystem);
		Text processText = UI.formText(settings, toolkit, M.Process);
		Text qRefText = UI.formText(settings, toolkit, M.QuantitativeReference);
		Text simCountText = UI.formText(settings, toolkit, M.NumberOfSimulations);
		if (editor.setup != null) {
			var setup = editor.setup;
			systemText.setText(Labels.name(setup.target()));
			processText.setText(Labels.name(setup.process()));
			qRefText.setText(getQRefText());
			simCountText.setText(Integer.toString(setup.numberOfRuns()));
		}
		systemText.setEditable(false);
		processText.setEditable(false);
		qRefText.setEditable(false);
		simCountText.setEditable(false);
	}

	private String getQRefText() {
		var setup = editor.setup;
		double amount = setup.amount();
		Flow flow = setup.flow();
		Unit unit = setup.unit();
		return String.format("%s %s %s",
			Numbers.format(amount, 2),
			Labels.name(unit),
			Labels.name(flow));
	}

	private void createProgressSection(FormToolkit toolkit, Composite body) {
		progressSection = UI.section(body, toolkit, M.Progress);
		Composite composite = UI.sectionClient(progressSection, toolkit);
		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setMaximum(editor.setup.numberOfRuns());
		UI.gridData(progressBar, false, false).widthHint = 470;
		Button progressButton = toolkit.createButton(composite,
			M.Start, SWT.NONE);
		UI.gridData(progressButton, false, false).widthHint = 70;
		new SimulationControl(progressButton, editor, this);
	}

	private void createResultSection(FormToolkit tk, Composite body) {
		if (result == null)
			return;
		Section section = UI.section(body, tk, M.Results);
		SimulationExportAction exportAction = new SimulationExportAction(
			result, editor.setup);
		Actions.bind(section, exportAction);
		Composite comp = UI.sectionClient(section, tk);
		initFlowCheckViewer(tk, comp);
		if (result.hasImpacts()) {
			initImpactCheckViewer(tk, comp);
		}
		statisticsCanvas = new StatisticsCanvas(body);
		GridData gd = UI.gridData(statisticsCanvas, true, true);
		gd.verticalIndent = 10;
		gd.minimumHeight = 250;
	}

	private void initImpactCheckViewer(FormToolkit toolkit, Composite section) {
		Button impactCheck = toolkit.createButton(section,
			M.ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(section);
		impactViewer.setEnabled(false);
		impactViewer.setInput(result.getImpacts());
		impactViewer.addSelectionChangedListener((e) -> updateSelection());
		impactViewer.selectFirst();
		new ResultTypeCheck<>(impactViewer, impactCheck, IMPACT);
	}

	private void initFlowCheckViewer(FormToolkit tk, Composite section) {
		Button flowsCheck = tk.createButton(section, M.Flows, SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new ResultFlowCombo(section);
		flowViewer.setInput(result.getFlows());
		flowViewer.selectFirst();
		flowViewer.addSelectionChangedListener((e) -> updateSelection());
		new ResultTypeCheck<>(flowViewer, flowsCheck, FLOW);
	}

	private void updateSelection() {
		if (result == null || statisticsCanvas == null)
			return;
		if (resultType == FLOW) {
			var flow = flowViewer.getSelected();
			if (flow == null)
				return;
			double[] vals = resultPin != null
				? result.getAllUpstream(resultPin, flow)
				: result.getAll(flow);
			statisticsCanvas.setValues(vals);
		} else {
			ImpactDescriptor cat = impactViewer.getSelected();
			if (cat == null)
				return;
			double[] vals = resultPin != null
				? result.getAllUpstream(resultPin, cat)
				: result.getAll(cat);
			statisticsCanvas.setValues(vals);
		}
	}

	void updateProgress() {
		if (result == null)
			return;
		updateSelection();
		progressBar.setSelection(progressBar.getSelection() + 1);
	}

	void progressDone() {
		progressSection.dispose();
		form.reflow(true);
	}

	private class ResultTypeCheck<T> implements SelectionListener {

		private final AbstractComboViewer<T> viewer;
		private final Button check;
		private final int type;

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
