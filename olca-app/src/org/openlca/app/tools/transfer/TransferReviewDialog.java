package org.openlca.app.tools.transfer;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.io.olca.systransfer.ProviderInfo;
import org.openlca.io.olca.systransfer.ProviderMatch;
import org.openlca.io.olca.systransfer.TransferPlan;

final class TransferReviewDialog extends FormDialog {

	private final TransferPlan plan;
	private org.eclipse.jface.viewers.TableViewer table;
	private Combo providerCombo;
	private Label providerInfo;
	private List<ProviderInfo> currentCandidates = List.of();

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
		var summary = UI.label(body, tk, summaryText());
		UI.gridData(summary, true, false);

		table = Tables.createViewer(body,
			"Provider",
			"Selected provider",
			"Status");
		Tables.bindColumnWidths2(table, 0.42, 0.42, 0.16);
		table.setLabelProvider(new MatchLabel());
		table.setInput(plan.matches());
		table.addSelectionChangedListener($ -> updateSelection());

		var bottom = UI.composite(body, tk);
		UI.gridLayout(bottom, 2);
		UI.gridData(bottom, true, false);
		providerInfo = UI.label(bottom, tk, "Select a row to review provider options");
		UI.gridData(providerInfo, true, false).horizontalSpan = 2;

		providerCombo = UI.labeledCombo(bottom, tk, "Provider");
		UI.gridData(providerCombo, true, false);
		Controls.onSelect(providerCombo, $ -> selectCandidate());
		updateProviderControls(null);

		if (!plan.matches().isEmpty()) {
			var first = plan.matches().getFirst();
			table.setSelection(new StructuredSelection(first), true);
			updateProviderControls(first);
		}
	}

	private void updateSelection() {
		var match = Viewers.getFirstSelected(table);
		updateProviderControls(match instanceof ProviderMatch m ? m : null);
	}

	private void updateProviderControls(ProviderMatch match) {
		if (providerCombo == null || providerCombo.isDisposed())
			return;

		currentCandidates = match != null ? match.alternatives() : List.of();
		var items = new String[currentCandidates.size()];
		for (int i = 0; i < currentCandidates.size(); i++) {
			items[i] = providerLabel(currentCandidates.get(i));
		}
		providerCombo.setItems(items);

		if (match == null) {
			providerInfo.setText("Select a row to review provider options");
			providerCombo.setEnabled(false);
			providerCombo.deselectAll();
			return;
		}

		providerInfo.setText("Provider: " + providerLabel(match.provider())
			+ " | Candidates: " + currentCandidates.size());
		providerCombo.setEnabled(!currentCandidates.isEmpty());

		if (match.selected() != null) {
			int idx = currentCandidates.indexOf(match.selected());
			if (idx >= 0) {
				providerCombo.select(idx);
				return;
			}
		}
		providerCombo.deselectAll();
	}

	private void selectCandidate() {
		var match = Viewers.getFirstSelected(table);
		if (!(match instanceof ProviderMatch row))
			return;
		int idx = providerCombo.getSelectionIndex();
		if (idx < 0 || idx >= currentCandidates.size())
			return;
		row.select(currentCandidates.get(idx));
		table.refresh(row);
		updateProviderControls(row);
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
			if (!(element instanceof ProviderMatch match))
				return null;
			return switch (columnIndex) {
				case 0 -> providerLabel(match.provider());
				case 1 -> providerLabel(match.selected());
				case 2 -> statusOf(match);
				default -> null;
			};
		}
	}

	private String summaryText() {
		return plan.matches().size() + " provider match"
			+ (plan.matches().size() == 1 ? "" : "es")
			+ " found; "
			+ plan.copies().size() + " provider"
			+ (plan.copies().size() == 1 ? "" : "s")
			+ " will be copied.";
	}

	private static String statusOf(ProviderMatch match) {
		if (match == null)
			return null;
		int count = match.alternatives().size();
		return count == 1 ? "Single candidate" : count + " candidates";
	}

	private static String providerLabel(ProviderInfo info) {
		return TransferProviderLabels.of(info);
	}
}
