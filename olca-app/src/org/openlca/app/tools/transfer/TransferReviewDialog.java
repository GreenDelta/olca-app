package org.openlca.app.tools.transfer;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;

final class TransferReviewDialog extends FormDialog {

	private final TransferPlan plan;
	private org.eclipse.jface.viewers.TableViewer table;
	private Combo providerCombo;
	private Label providerInfo;
	private Button clearButton;
	private List<ProviderCandidate> currentCandidates = List.of();

	static boolean show(TransferPlan plan) {
		return new TransferReviewDialog(plan).open() == OK;
	}

	private TransferReviewDialog(TransferPlan plan) {
		super(UI.shell());
		this.plan = plan;
		setBlockOnOpen(true);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Review provider matching");
	}

	@Override
	protected Point getInitialSize() {
		return UI.initialSizeOf(this, 900, 550);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1);

		table = Tables.createViewer(body,
			"Provider",
			"Selected provider",
			"Status");
		Tables.bindColumnWidths2(table, 0.42, 0.42, 0.16);
		table.setLabelProvider(new MatchLabel());
		table.setInput(plan.matches());
		table.addSelectionChangedListener($ -> updateSelection());

		var bottom = UI.composite(body, tk);
		UI.gridLayout(bottom, 3);
		UI.gridData(bottom, true, false);
		providerInfo = UI.label(bottom, tk, "Select a row to review provider options");
		UI.gridData(providerInfo, true, false).horizontalSpan = 3;

		providerCombo = UI.labeledCombo(bottom, tk, "Provider");
		UI.gridData(providerCombo, true, false).horizontalSpan = 2;
		Controls.onSelect(providerCombo, $ -> selectCandidate());

		clearButton = tk.createButton(bottom, "Clear", SWT.PUSH);
		clearButton.addListener(SWT.Selection, $ -> clearSelection());
		updateProviderControls(null);

		if (!plan.matches().isEmpty()) {
			var first = plan.matches().getFirst();
			table.setSelection(new StructuredSelection(first), true);
			updateProviderControls(first);
		}
	}

	private void updateSelection() {
		var match = Viewers.getFirstSelected(table);
		updateProviderControls(match instanceof TransferMatch m ? m : null);
	}

	private void updateProviderControls(TransferMatch match) {
		if (providerCombo == null || providerCombo.isDisposed())
			return;

		currentCandidates = match != null ? match.candidates() : List.of();
		var items = new String[currentCandidates.size()];
		for (int i = 0; i < currentCandidates.size(); i++) {
			items[i] = currentCandidates.get(i).label();
		}
		providerCombo.setItems(items);

		if (match == null) {
			providerInfo.setText("Select a row to review provider options");
			providerCombo.setEnabled(false);
			providerCombo.deselectAll();
			clearButton.setEnabled(false);
			return;
		}

		providerInfo.setText("Provider: " + match.providerLabel() + " | Candidates: "
			+ currentCandidates.size());
		providerCombo.setEnabled(!currentCandidates.isEmpty());
		clearButton.setEnabled(match.selectedCandidate() != null);

		if (match.selectedCandidate() != null) {
			int idx = currentCandidates.indexOf(match.selectedCandidate());
			if (idx >= 0) {
				providerCombo.select(idx);
				return;
			}
		}
		providerCombo.deselectAll();
	}

	private void selectCandidate() {
		var match = Viewers.getFirstSelected(table);
		if (!(match instanceof TransferMatch row))
			return;
		int idx = providerCombo.getSelectionIndex();
		if (idx < 0 || idx >= currentCandidates.size())
			return;
		row.select(currentCandidates.get(idx));
		clearButton.setEnabled(true);
		table.refresh(row);
	}

	private void clearSelection() {
		var match = Viewers.getFirstSelected(table);
		if (!(match instanceof TransferMatch row))
			return;
		row.select(null);
		providerCombo.deselectAll();
		clearButton.setEnabled(false);
		table.refresh(row);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		var ok = getButton(IDialogConstants.OK_ID);
		if (ok != null) {
			ok.setText("Transfer");
		}
	}

	private static class MatchLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof TransferMatch match))
				return null;
			return switch (columnIndex) {
				case 0 -> match.providerLabel();
				case 1 -> match.selectedLabel();
				case 2 -> match.status();
				default -> null;
			};
		}
	}
}
