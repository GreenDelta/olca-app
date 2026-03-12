package org.openlca.app.editors.sd;

import java.util.UUID;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.util.Controls;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.ImpactMethodViewer;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.sd.model.EntityRef;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.SimSpecs;

public class SdModelWizard extends FormDialog {

	private final IDatabase db;
	private final SdModel model;

	private SdModelWizard(IDatabase db) {
		super(UI.shell());
		this.db = db;
		model = new SdModel();
		model.setId(UUID.randomUUID().toString());
		model.setName("New system dynamics model");
		model.setSimSpecs(new SimSpecs(1, 10, 1, "years"));
	}

	public static void openNew() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NoDatabaseOpenedErr);
			return;
		}
		new SdModelWizard(db).open();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 400);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("New system dynamics model");
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		var comp = UI.composite(body, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, false);

		var nameText = UI.labeledText(comp, tk, M.Name);
		Controls.set(nameText, model.name(), name -> {
			model.setName(name);
			validate();
		});

		var startText = UI.labeledText(comp, tk, "Start time");
		Controls.set(startText, model.simSpecs().start(), start -> {
			model.simSpecs().setStart(start);
			validate();
		});

		var endText = UI.labeledText(comp, tk, "Stop time");
		Controls.set(endText, model.simSpecs().end(), end -> {
			model.simSpecs().setEnd(end);
			validate();
		});

		var dtText = UI.labeledText(comp, tk, "Time step (Δt)");
		Controls.set(dtText, model.simSpecs().dt(), dt -> {
			model.simSpecs().setDt(dt);
			validate();
		});

		var unitText = UI.labeledText(comp, tk, "Time unit");
		Controls.set(unitText, model.simSpecs().unit(),
			unit -> model.simSpecs().setUnit(unit));

		UI.label(comp, tk, M.ImpactAssessmentMethod);
		var methodCombo = new ImpactMethodViewer(comp);
		UI.gridData(methodCombo.getControl(), true, false).widthHint = 1;
		methodCombo.setInput(db);
		methodCombo.addSelectionChangedListener(
			d -> model.lca().impactMethod(EntityRef.of(d)));
	}

	private void validate() {
		var btn = getButton(OK);
		if (btn == null) return;
		btn.setEnabled(canCreate());
	}

	private boolean canCreate() {
		if (Strings.isBlank(model.name())) {
			return false;
		}
		var specs = model.simSpecs();
		return specs.start() >= 0
			&& specs.end() > specs.start()
			&& specs.dt() > 0;
	}

	@Override
	protected void okPressed() {
		var res = SystemDynamics.saveModel(model, db);
		if (res.isError()) {
			MsgBox.error("Failed to create model", res.error());
			return;
		}
		SdModelEditor.open(res.value());
		Navigator.refresh();
		super.okPressed();
	}
}
