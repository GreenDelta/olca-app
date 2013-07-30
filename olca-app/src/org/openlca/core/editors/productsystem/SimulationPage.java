package org.openlca.core.editors.productsystem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.UI;
import org.openlca.app.viewer.AbstractViewer;
import org.openlca.app.viewer.FlowViewer;
import org.openlca.app.viewer.ISelectionChangedListener;
import org.openlca.app.viewer.ImpactCategoryViewer;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.math.SimulationResult;
import org.openlca.core.math.SimulationSolver;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

public class SimulationPage extends FormPage {

	private final int FLOW = 0;
	private final int IMPACT = 1;
	private int resultType = FLOW;

	private StatisticsCanvas statisticsCanvas;
	private ProgressBar progressBar;
	private SimulationInput input;
	private FlowViewer flowViewer;
	private Section progressSection;
	private ScrolledForm form;
	private SimulationResult result;
	private ImpactCategoryViewer impactViewer;
	private IDatabase database;

	public SimulationPage(SimulationEditor editor, String id, String title,
			IDatabase database) {
		super(editor, id, title);
		this.database = database;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(Messages.Common_MonteCarloSimulation);
		toolkit.decorateFormHeading(form.getForm());
		Composite body = UI.formBody(form, toolkit);
		createSettingsSection(toolkit, body);
		if (input != null && input.getSolver() != null
				&& input.getSolver().canRun())
			createProgressSection(toolkit, body);
		createResultSection(toolkit, body);
		form.pack();
	}

	private void createSettingsSection(FormToolkit toolkit, Composite body) {
		Composite settings = UI.formSection(body, toolkit,
				Messages.Common_Settings);
		Text systemText = UI.formText(settings, toolkit,
				Messages.Common_ProductSystem);
		Text processText = UI.formText(settings, toolkit,
				Messages.Common_Process);
		Text qRefText = UI.formText(settings, toolkit,
				Messages.Common_QuantitativeReference);
		Text simCountText = UI.formText(settings, toolkit,
				Messages.Simulation_NumberOfSimulations);
		if (input != null) {
			systemText.setText(input.getName());
			processText.setText(input.getReferenceProcessName());
			qRefText.setText(input.getQuantitativeReference());
			simCountText.setText(Integer.toString(input.getNumberOfRuns()));
		}
		systemText.setEditable(false);
		processText.setEditable(false);
		qRefText.setEditable(false);
		simCountText.setEditable(false);
	}

	private void createProgressSection(FormToolkit toolkit, Composite body) {
		progressSection = UI.section(body, toolkit, Messages.Common_Progress);
		Composite composite = UI.sectionClient(progressSection, toolkit);
		progressBar = new ProgressBar(composite, SWT.SMOOTH);
		progressBar.setMaximum(input.getNumberOfRuns());
		UI.gridWidth(progressBar, 470);
		final Button progressButton = toolkit.createButton(composite,
				Messages.Common_Start, SWT.NONE);
		UI.gridWidth(progressButton, 70);
		new SimulationControl(progressButton, input, this);
	}

	private void createResultSection(FormToolkit toolkit, Composite body) {
		if (result == null)
			return;
		Section section = UI.section(body, toolkit, Messages.Common_Results);
		SimulationExportAction exportAction = new SimulationExportAction();
		exportAction.configure(input, result);
		UI.bindActions(section, exportAction);
		Composite composite = UI.sectionClient(section, toolkit);
		initFlowCheckViewer(toolkit, composite);
		if (result.hasImpactResults())
			initImpactCheckViewer(toolkit, composite);
		statisticsCanvas = new StatisticsCanvas(body);
		UI.gridData(statisticsCanvas, true, true).verticalIndent = 5;
	}

	private void initImpactCheckViewer(FormToolkit toolkit, Composite section) {
		Button impactCheck = toolkit.createButton(section,
				Messages.Common_ImpactCategories, SWT.RADIO);
		impactViewer = new ImpactCategoryViewer(section);
		impactViewer.setEnabled(false);
		impactViewer.setInput(result);
		impactViewer
				.addSelectionChangedListener(new SelectionChange<ImpactCategoryDescriptor>());
		impactViewer.selectFirst();
		new ResultTypeCheck<>(impactViewer, impactCheck, IMPACT);
	}

	private void initFlowCheckViewer(FormToolkit toolkit, Composite section) {
		Button flowsCheck = toolkit.createButton(section,
				Messages.Common_Flows, SWT.RADIO);
		flowsCheck.setSelection(true);
		flowViewer = new FlowViewer(section);
		flowViewer.setInput(result);
		flowViewer.selectFirst();
		flowViewer.addSelectionChangedListener(new SelectionChange<Flow>());
		new ResultTypeCheck<>(flowViewer, flowsCheck, FLOW);
	}

	private void updateSelection() {
		if (result == null || statisticsCanvas == null)
			return;
		if (resultType == FLOW) {
			Flow flow = flowViewer.getSelected();
			if (flow != null)
				statisticsCanvas.setValues(result.getResults(flow));
		} else {
			ImpactCategoryDescriptor cat = impactViewer.getSelected();
			if (cat != null)
				statisticsCanvas.setValues(result.getResults(cat));
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

	@Override
	public void init(IEditorSite site, IEditorInput input) {
		super.init(site, input);
		if (input instanceof SimulationInput) {
			this.input = (SimulationInput) input;
			SimulationSolver solver = this.input.getSolver();
			if (solver != null)
				this.result = solver.getResult();
		}
	}

	private class SelectionChange<T> implements ISelectionChangedListener<T> {
		@Override
		public void selectionChanged(T selection) {
			updateSelection();
		}
	}

	private class ResultTypeCheck<T> implements SelectionListener {

		private AbstractViewer<T> viewer;
		private Button check;
		private int type;

		public ResultTypeCheck(AbstractViewer<T> viewer, Button check, int type) {
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
