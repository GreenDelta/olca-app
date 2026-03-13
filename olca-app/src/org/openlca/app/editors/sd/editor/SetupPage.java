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
import org.openlca.sd.interop.Progress;
import org.openlca.sd.model.EntityRef;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.SimSpecs;

import java.util.concurrent.atomic.AtomicInteger;

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

		var specs = specs();
		var unit = Strings.isNotBlank(specs.unit())
			? specs.unit()
			: "";

		var startText = UI.labeledText(comp, tk, "Start time");
		startText.setText(Double.toString(specs.start()));
		UI.label(comp, tk, unit);
		startText.addModifyListener(e -> {
			try {
				specs().setStart(Double.parseDouble(startText.getText()));
				editor.setDirty();
			} catch (Exception ignored) {
			}
		});

		var endText = UI.labeledText(comp, tk, "Stop time");
		endText.setText(Double.toString(specs.end()));
		UI.label(comp, tk, unit);
		endText.addModifyListener(e -> {
			try {
				specs().setEnd(Double.parseDouble(endText.getText()));
				editor.setDirty();
			} catch (Exception ignored) {
			}
		});

		var dtText = UI.labeledText(comp, tk, "Δt");
		dtText.setText(Double.toString(specs.dt()));
		UI.label(comp, tk, unit);
		dtText.addModifyListener(e -> {
			try {
				specs().setDt(Double.parseDouble(dtText.getText()));
				editor.setDirty();
			} catch (Exception ignored) {
			}
		});

		var unitText = UI.labeledText(comp, tk, "Time unit");
		unitText.setText(unit);
		UI.filler(comp, tk);
		unitText.addModifyListener(e -> {
			specs().setUnit(unitText.getText().strip());
			editor.setDirty();
		});

		createMethodCombo(comp, tk);
		UI.filler(comp, tk);

		UI.filler(comp, tk);
		var btn = UI.button(comp, tk, "Run simulation");
		btn.setImage(Icon.RUN.get());
		Controls.onSelect(btn, e -> runSimulation());
	}

	private SimSpecs specs() {
		var specs = model.simSpecs();
		if (specs == null) {
			specs = new SimSpecs();
			model.setSimSpecs(specs);
		}
		return specs;
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
			int totalWork = model.simSpecs() != null
				? model.simSpecs().iterationCount()
				: IProgressMonitor.UNKNOWN;

			service.run(true, true, monitor -> {
				monitor.beginTask("Running simulation", totalWork);

				var iteration = new AtomicInteger(0);
				sim.run(new Progress() {
					@Override
					public void tick() {
						monitor.worked(iteration.incrementAndGet());
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
}
