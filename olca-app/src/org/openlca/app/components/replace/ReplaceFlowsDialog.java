package org.openlca.app.components.replace;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowViewer;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.util.FlowReplacer;

public class ReplaceFlowsDialog extends FormDialog {

	private final IDatabase db;
	private final List<FlowDescriptor> usedFlows;

	private FlowViewer sourceCombo;
	private FlowViewer targetCombo;
	private Button processesCheck;
	private Button impactsCheck;
	private Button replaceBothButton;

	public static void openDialog() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var flows = App.exec("Collect used flows...", () -> {
			var all = FlowReplacer.getUsedFlowsOf(db);
			all.sort(
				(f1, f2) -> Strings.compareIgnoreCase(Labels.name(f1), Labels.name(f2)));
			return all;
		});
		if (flows.size() < 2) {
			MsgBox.info(
					"No replaceable flows found",
					"There are no used flows in the database that could be replaced");
			return;
		}
		new ReplaceFlowsDialog(db, flows).open();
	}

	private ReplaceFlowsDialog(IDatabase db, List<FlowDescriptor> usedFlows) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.db = db;
		this.usedFlows = usedFlows;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(M.BulkreplaceFlows);
		newShell.setSize(800, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		UI.gridLayout(body, 1, 0, 20);
		createTop(body, tk);
		createBottom(body, tk);
	}

	private void createTop(Composite parent, FormToolkit tk) {
		var top = UI.composite(parent, tk);
		UI.gridLayout(top, 2, 20, 5);
		UI.gridData(top, true, false);

		UI.label(top, tk,  M.ReplaceFlow);
		sourceCombo = new FlowViewer(top);
		sourceCombo.addSelectionChangedListener(this::updateReplacementCandidates);

		UI.label(top, tk, M.With);
		targetCombo = new FlowViewer(top);
		targetCombo.addSelectionChangedListener($ -> updateButtons());
		targetCombo.setEnabled(false);

		App.runInUI("Render flows", () -> sourceCombo.setInput(usedFlows));
		tk.paintBordersFor(top);
	}


	private void updateReplacementCandidates(FlowDescriptor selected) {
		var candidates = App.exec(
				"Find candidates...", () -> FlowReplacer.getCandidatesOf(db, selected));
		candidates.sort(
			(f1, f2) -> Strings.compareIgnoreCase(Labels.name(f1), Labels.name(f2)));
		targetCombo.setInput(candidates);
		if (candidates.size() == 1) {
			targetCombo.select(candidates.getFirst());
		}
		targetCombo.setEnabled(candidates.size() > 1);
		updateButtons();
	}

	private void createBottom(Composite parent, FormToolkit tk) {
		var bottom = UI.composite(parent, tk);
		UI.gridLayout(bottom, 1, 0, 0);
		Composite typeContainer = UI.composite(bottom, tk);
		UI.gridLayout(typeContainer, 4, 20, 5);
		UI.label(typeContainer, tk, M.ReplaceIn);
		processesCheck = UI.radio(typeContainer, tk, M.InputsOutputs);
		processesCheck.setSelection(true);
		Controls.onSelect(processesCheck, this::updateSelection);
		impactsCheck = UI.radio(typeContainer, tk, M.ImpactFactors);
		Controls.onSelect(impactsCheck, this::updateSelection);
		replaceBothButton = UI.radio(typeContainer, tk, M.Both);
		Controls.onSelect(replaceBothButton, this::updateSelection);
		tk.paintBordersFor(bottom);
	}

	private void updateSelection(SelectionEvent e) {
		if (!(e.getSource() instanceof Button source))
			return;
		if (!source.getSelection())
			return;
		var buttons = new Button[]{
				processesCheck,
				impactsCheck,
				replaceBothButton
		};
		for (var button : buttons) {
			if (source == button)
				continue;
			button.setSelection(false);
		}
	}

	private void updateButtons() {
		FlowDescriptor first = sourceCombo.getSelected();
		FlowDescriptor second = targetCombo.getSelected();
		boolean enabled = first != null
				&& first.id != 0L
				&& second != null
				&& second.id != 0L;
		getButton(IDialogConstants.OK_ID).setEnabled(enabled);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateButtons();
	}

	@Override
	protected void okPressed() {
		var oldFlow = sourceCombo.getSelected();
		var newFlow = targetCombo.getSelected();

		var replacer = FlowReplacer.of(db);
		if (replaceBothButton.getSelection()) {
			replacer.replaceIn(ModelType.PROCESS, ModelType.IMPACT_CATEGORY);
		} else if (processesCheck.getSelection()) {
			replacer.replaceIn(ModelType.PROCESS);
		} else if (impactsCheck.getSelection()) {
			replacer.replaceIn(ModelType.IMPACT_CATEGORY);
		}

		super.okPressed();
		App.runWithProgress("Replace flow", () -> replacer.replace(oldFlow, newFlow));
	}

}
