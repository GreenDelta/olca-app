package org.openlca.app.editors.sd.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.sd.interop.CoupledSimulator;
import org.openlca.sd.interop.SimulationSetup;
import org.openlca.app.editors.sd.results.SdResultEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.sd.eqn.TimeSeq;
import org.openlca.sd.xmile.Xmile;

class SetupPage extends FormPage {

	private final SdModelEditor editor;
	private final SimulationSetup setup;
	private final IDatabase db;

	SetupPage(SdModelEditor editor) {
		super(editor, "SdSetupPage", M.CalculationSetup);
		this.editor = editor;
		this.setup = editor.setup();
		this.db = editor.db();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + editor.modelName());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		infoSection(body, tk);
		new BindingsPanel(body, editor, tk, form);
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 3);

		var nameText = UI.labeledText(comp, tk, M.Name);
		nameText.setEditable(false);
		nameText.setText(editor.modelName());
		UI.filler(comp, tk);

		var specs = SimSpecs.of(editor.xmile());

		var methodText = UI.labeledText(comp, tk, "Solver method");
		methodText.setEditable(false);
		methodText.setText(specs.method);
		UI.filler(comp, tk);

		var startText = UI.labeledText(comp, tk, "Start time");
		startText.setEditable(false);
		startText.setText(Double.toString(specs.start));
		UI.label(comp, tk, specs.timeUnit);

		var endText = UI.labeledText(comp, tk, "Stop time");
		endText.setEditable(false);
		endText.setText(Double.toString(specs.stop));
		UI.label(comp, tk, specs.timeUnit);

		var dtText = UI.labeledText(comp, tk, "Δt");
		dtText.setEditable(false);
		dtText.setText(Double.toString(specs.dt));
		UI.label(comp, tk, specs.timeUnit);

		createMethodCombo(comp, tk);
		UI.filler(comp, tk);

		UI.filler(comp, tk);
		var btn = UI.button(comp, tk, "Run simulation");
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> runSimulation());
	}

	private void createMethodCombo(Composite comp, FormToolkit tk) {
		UI.label(comp, tk, M.ImpactAssessmentMethod);
		var combo = new ImpactMethodViewer(comp);
		var methods = new ImpactMethodDao(db)
			.getDescriptors()
			.stream()
			.sorted((m1, m2) -> Strings.compareIgnoreCase(m1.name, m2.name))
			.toList();
		combo.setInput(methods);
		if (setup.method() != null) {
			combo.select(Descriptor.of(setup.method()));
		}
		combo.addSelectionChangedListener(d -> {
			if (d == null)
				return;
			setup.method(db.get(ImpactMethod.class, d.id));
			editor.setDirty();
		});
	}

	private void runSimulation() {
		var calculator = new SystemCalculator(Database.get())
			.withSolver(App.getSolver());
		Libraries.readersForCalculation()
			.ifPresent(calculator::withLibraries);

		var simRes = CoupledSimulator.of(
			editor.xmile(), editor.setup(), calculator);
		if (simRes.isError()) {
			MsgBox.error("Failed to create simulator", simRes.error());
			return;
		}
		var sim = simRes.value();

		try {
			var service = PlatformUI.getWorkbench().getProgressService();

			service.run(true, true, monitor -> {
				monitor.beginTask(
					"Running simulation",
					iterationCountOf(editor.xmile()));

				int[] iteration = { 0 };
				sim.run(new CoupledSimulator.Progress() {

					@Override
					public void worked(int work) {
						iteration[0]++;
						monitor.subTask("Run iteration " + iteration[0]);
						monitor.worked(work);
					}

					@Override
					public boolean isCanceled() {
						return monitor.isCanceled();
					}
				});
				monitor.done();
				App.runInUI("Open simulation result", () -> {
					var res = sim.getResult();
					if (res.isError()) {
						MsgBox.error("Simulation failed", res.error());
					} else {
						SdResultEditor.open(editor.modelName(), res.value());
					}
				});
			});

		} catch (Exception e) {
			ErrorReporter.on("Failed to run simulation", e);
		}
	}

	private int iterationCountOf(Xmile xmile) {
		var seq = TimeSeq.of(xmile);
		return seq.isError()
			? IProgressMonitor.UNKNOWN
			: seq.value().iterationCount();
	}

	private record SimSpecs(
		double start,
		double stop,
		double dt,
		String timeUnit,
		String method) {

		static SimSpecs of(Xmile xmile) {
			if (xmile == null || xmile.simSpecs() == null)
				return new SimSpecs(0, 0, 0, "", "");
			var specs = xmile.simSpecs();
			double dt = 1;
			if (specs.dt() != null && specs.dt().value() != null) {
				dt = specs.dt().reciprocal() != null && specs.dt().reciprocal()
					? 1 / specs.dt().value()
					: specs.dt().value();
			}

			return new SimSpecs(
				specs.start() != null ? specs.start() : 0,
				specs.stop() != null ? specs.stop() : 0,
				dt,
				specs.timeUnits() != null ? specs.timeUnits() : "",
				specs.method() != null ? specs.method() : "");
		}
	}

}
