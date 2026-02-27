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
import org.openlca.sd.interop.CoupledSimulator;
import org.openlca.sd.model.EntityRef;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.SimSpecs;

class SetupPage extends FormPage {

	private final SdModelEditor editor;
	private final SdModel model;
	private final IDatabase db;

	SetupPage(SdModelEditor editor) {
		super(editor, "SdSetupPage", M.CalculationSetup);
		this.editor = editor;
		this.model = editor.model();
		this.db = editor.db();
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var form = UI.header(mForm, "System dynamics model: " + model.name());
		var tk = mForm.getToolkit();
		var body = UI.body(form, tk);
		infoSection(body, tk);
		new BindingsPanel(body, editor, tk, form);
	}

	private void infoSection(Composite body, FormToolkit tk) {
		var comp = UI.formSection(body, tk, M.GeneralInformation);
		UI.gridLayout(comp, 3);

		var nameText = UI.labeledText(comp, tk, M.Name);
		Controls.set(nameText, model.name());
		UI.filler(comp, tk);
		nameText.addModifyListener(e -> {
			var name = nameText.getText().strip();
			if (!name.isEmpty()) {
				model.setName(name);
				editor.setDirty();
			}
		});

		var time = model.time();
		var unit = time != null && time.unit() != null
			? time.unit()
			: "";

		var startText = UI.labeledText(comp, tk, "Start time");
		startText.setText(Double.toString(time != null ? time.start() : 0));
		UI.label(comp, tk, unit);
		startText.addModifyListener(e -> {
			try {
				time().setStart(Double.parseDouble(startText.getText()));
				editor.setDirty();
			} catch (Exception ignored) {
			}
		});

		var endText = UI.labeledText(comp, tk, "Stop time");
		endText.setText(Double.toString(time != null ? time.end() : 0));
		UI.label(comp, tk, unit);
		endText.addModifyListener(e -> {
			try {
				time().setEnd(Double.parseDouble(endText.getText()));
				editor.setDirty();
			} catch (Exception ignored) {
			}
		});

		var dtText = UI.labeledText(comp, tk, "Δt");
		dtText.setText(Double.toString(time != null ? time.dt() : 1));
		UI.label(comp, tk, unit);
		dtText.addModifyListener(e -> {
			try {
				time().setDt(Double.parseDouble(dtText.getText()));
				editor.setDirty();
			} catch (Exception ignored) {
			}
		});

		var unitText = UI.labeledText(comp, tk, "Time unit");
		unitText.setText(unit);
		UI.filler(comp, tk);
		unitText.addModifyListener(e -> {
			time().setUnit(unitText.getText().strip());
			editor.setDirty();
		});

		createMethodCombo(comp, tk);
		UI.filler(comp, tk);

		UI.filler(comp, tk);
		var btn = UI.button(comp, tk, "Run simulation");
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> runSimulation());
	}

	private SimSpecs time() {
		var time = model.time();
		if (time == null) {
			time = new SimSpecs();
			model.setTime(time);
		}
		return time;
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
		var methodRef = model.lca().impactMethod();
		if (methodRef != null) {
			methods.stream()
				.filter(m -> methodRef.refId().equals(m.refId))
				.findFirst()
				.ifPresent(combo::select);
		}
		combo.addSelectionChangedListener(d -> {
			if (d == null) {
				model.lca().impactMethod(null);
			} else {
				model.lca().impactMethod(EntityRef.of(d));
			}
			editor.setDirty();
		});
	}

	private void runSimulation() {
		var calculator = new SystemCalculator(Database.get())
			.withSolver(App.getSolver());
		Libraries.readersForCalculation()
			.ifPresent(calculator::withLibraries);

		var simRes = CoupledSimulator.of(
			model, db, calculator);
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
					iterationCount());

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
						SdResultEditor.open(model.name(), res.value());
					}
				});
			});

		} catch (Exception e) {
			ErrorReporter.on("Failed to run simulation", e);
		}
	}

	private int iterationCount() {
		var time = model.time();
		return time != null
			? time.iterationCount()
			: IProgressMonitor.UNKNOWN;
	}

}
