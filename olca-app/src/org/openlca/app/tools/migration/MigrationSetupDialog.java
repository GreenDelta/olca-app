package org.openlca.app.tools.migration;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.components.ModelCheckBoxTree;
import org.openlca.app.util.Controls;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.model.ModelType;
import org.openlca.io.olca.migration.MigrationPlan;

public class MigrationSetupDialog extends FormDialog {

	private final MigrationSetup config;

	public static void show() {

		// initialize the migration setup
		var res = MigrationSetup.initialize();
		if (res.isError()) {
			MsgBox.error("Cannot create migration", res.error());
			return;
		}
		var setup = res.value();
		var dialog = new MigrationSetupDialog(setup);
		if (dialog.open() != OK || !setup.isComplete())
			return;

		// open the target database of the configuration
		var confRes = App.exec("Open target database", setup::openConfig);
		if (confRes.isError()) {
			MsgBox.error("Cannot create migration", confRes.error());
			return;
		}
		var config = confRes.value();
		var target = config.target();

		// initialize the migration plan and open it in the editor
		try (target) {
			var planRes = App.exec(
				"Prepare migration plan", () -> MigrationPlan.createFrom(config));
			if (planRes.isError()) {
				MsgBox.error("Failed to create migration plan", planRes.error());
				return;
			}
			var cmd = new MigrationCommand(
				planRes.value(), config, setup.targetConfig());
			MigrationPlanEditor.open(cmd);
		} catch (Exception e) {
			ErrorReporter.on("Failed to create migration plan", e);
		}
	}

	private MigrationSetupDialog(MigrationSetup config) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.config = config;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Migration setup");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(700, 550);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1);

		var dataGroup = UI.group(body, tk);
		dataGroup.setText("Data for migration");
		UI.stretchXY(dataGroup);
		UI.gridLayout(dataGroup, 1);

		var allProcessesBtn = UI.checkbox(dataGroup, tk,
			"Copy all foreground processes");
		Controls.onSelect(allProcessesBtn, $ -> {
			config.setAllProcesses(allProcessesBtn.getSelection());
			updateOk();
		});

		var entityTree = new ModelCheckBoxTree(
			ModelType.PROJECT, ModelType.PRODUCT_SYSTEM, ModelType.IMPACT_METHOD)
			.withoutLibraries()
			.drawOn(dataGroup, tk);
		entityTree.onSelectionChanged(() -> {
			config.setEntities(entityTree.getSelection());
			updateOk();
		});

		var targetGroup = UI.group(body, tk);
		UI.stretchX(targetGroup);
		targetGroup.setText("Target database");
		UI.gridLayout(targetGroup, 1);

		createTargetCombo(targetGroup, tk);
		StrategyList.create(targetGroup, tk, config, this::updateOk);
		updateOk();
	}

	private void createTargetCombo(Composite parent, FormToolkit tk) {
		var combo = UI.combo(parent, tk);
		var targets = config.targets();
		var items = new String[targets.size()];
		for (int i = 0; i < targets.size(); i++) {
			items[i] = targets.get(i).name();
		}
		combo.setItems(items);
		combo.select(0);
		config.setTarget(targets.getFirst());
		Controls.onSelect(combo, $ -> {
			int idx = combo.getSelectionIndex();
			config.setTarget(targets.get(idx));
			updateOk();
		});
	}

	private void updateOk() {
		var button = getButton(IDialogConstants.OK_ID);
		if (button == null) return;
		button.setEnabled(config.isComplete());
	}
}
