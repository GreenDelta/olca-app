package org.openlca.app.components.replace;

import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
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
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.util.FlowReplacer;
import org.openlca.util.Strings;

public class ReplaceFlowsDialog extends FormDialog {

	private final IDatabase db;
	private final List<FlowDescriptor> usedFlows;

	private FlowViewer selectionViewer;
	private FlowViewer replacementViewer;
	private Button excludeWithProviders;
	private Button replaceFlowsButton;
	private Button replaceImpactsButton;
	private Button replaceBothButton;

	public static void openDialog() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var flows = App.exec("Find used flows...", () -> {
			var all = FlowReplacer.getUsedFlowsOf(db);
			all.sort((f1, f2) -> Strings.compare(Labels.name(f1), Labels.name(f2)));
			return all;
		});
		if (flows.size() < 2) {
			MsgBox.info(
					"No used flows found",
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
		selectionViewer = createFlowViewer(top, tk, M.ReplaceFlow, this::updateReplacementCandidates);
		replacementViewer = createFlowViewer(top, tk, M.With, selected -> updateButtons());
		replacementViewer.setEnabled(false);
		selectionViewer.setInput(usedFlows);
		tk.paintBordersFor(top);
	}

	private FlowViewer createFlowViewer(Composite parent, FormToolkit toolkit, String label,
			Consumer<FlowDescriptor> onChange) {
		UI.label(parent, toolkit, label);
		FlowViewer viewer = new FlowViewer(parent);
		viewer.addSelectionChangedListener(onChange);
		return viewer;
	}

	private void updateReplacementCandidates(FlowDescriptor selected) {
		var candidates = FlowReplacer.getCandidatesOf(db, selected);
		candidates.sort((f1, f2) -> Strings.compare(Labels.name(f1), Labels.name(f2)));
		replacementViewer.setInput(candidates);
		if (candidates.size() == 1) {
			replacementViewer.select(candidates.getFirst());
		}
		replacementViewer.setEnabled(candidates.size() > 1);
		updateButtons();
	}

	private void createBottom(Composite parent, FormToolkit toolkit) {
		var bottom = UI.composite(parent, toolkit);
		UI.gridLayout(bottom, 1, 0, 0);
		Composite typeContainer = UI.composite(bottom, toolkit);
		UI.gridLayout(typeContainer, 4, 20, 5);
		UI.label(typeContainer, toolkit, M.ReplaceIn);
		replaceFlowsButton = UI.radio(typeContainer, toolkit, M.InputsOutputs);
		replaceFlowsButton.setSelection(true);
		Controls.onSelect(replaceFlowsButton, this::updateSelection);
		replaceImpactsButton = UI.radio(typeContainer, toolkit, M.ImpactFactors);
		Controls.onSelect(replaceImpactsButton, this::updateSelection);
		replaceBothButton = UI.radio(typeContainer, toolkit, M.Both);
		Controls.onSelect(replaceBothButton, this::updateSelection);
		Composite excludeContainer = UI.composite(bottom, toolkit);
		UI.gridLayout(excludeContainer, 2, 20, 5);
		excludeWithProviders = UI.checkbox(excludeContainer, toolkit);
		UI.label(excludeContainer, toolkit, M.ExcludeExchangesWithDefaultProviders);
		toolkit.paintBordersFor(bottom);
		createNote(parent, toolkit);
	}

	private void updateSelection(SelectionEvent e) {
		if (!(e.getSource() instanceof Button source))
			return;
		if (!source.getSelection())
			return;
		var buttons = new Button[]{
				replaceFlowsButton,
				replaceImpactsButton,
				replaceBothButton
		};
		for (var button : buttons) {
			if (source == button)
				continue;
			button.setSelection(false);
		}
		excludeWithProviders.setEnabled(
				source == replaceFlowsButton || source == replaceBothButton);
	}

	private void createNote(Composite parent, FormToolkit toolkit) {
		String note = M.NoteDefaultProviders;
		Label noteLabel = UI.label(parent, toolkit, note, SWT.WRAP);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd.widthHint = 300;
		noteLabel.setLayoutData(gd);
	}

	private void updateButtons() {
		FlowDescriptor first = selectionViewer.getSelected();
		FlowDescriptor second = replacementViewer.getSelected();
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
		var oldFlow = selectionViewer.getSelected();
		var newFlow = replacementViewer.getSelected();

		var replacer = FlowReplacer.of(db);
		if (replaceBothButton.getSelection()) {
			replacer.replaceIn(ModelType.PROCESS, ModelType.IMPACT_CATEGORY);
		} else if (replaceFlowsButton.getSelection()) {
			replacer.replaceIn(ModelType.PROCESS);
		} else if (replaceImpactsButton.getSelection()) {
			replacer.replaceIn(ModelType.IMPACT_CATEGORY);
		}

		super.okPressed();
		App.runWithProgress("Replace flow", () -> replacer.replace(oldFlow, newFlow));
	}

}
