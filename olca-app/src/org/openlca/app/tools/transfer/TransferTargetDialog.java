package org.openlca.app.tools.transfer;

import java.util.function.Supplier;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class TransferTargetDialog extends FormDialog {

	private final TargetSelection config;

	public static void show() {

		var res = TargetSelection.load();
		if (res.isError()) {
			MsgBox.error("Cannot transfer a product system", res.error());
			return;
		}

		var config = res.value();
		var dialog = new TransferTargetDialog(config);
		if (dialog.open() != OK || !config.isComplete())
			return;

		var confRes = config.openConfig();
		if (confRes.isError()) {
			MsgBox.error("Cannot transfer a product system", confRes.error());
			return;
		}

		try (var transfer = confRes.value()) {
			@SuppressWarnings("unchecked")
			var planRes = new Res[1];
			App.runWithProgress("Prepare transfer", () ->
				planRes[0] = TransferPlanner.plan(transfer));
			if (planRes[0] == null || planRes[0].isError()) {
				var error = planRes[0] != null
					? planRes[0].error()
					: "Failed to prepare the transfer";
				MsgBox.error("Cannot transfer a product system", error);
				return;
			}

			var plan = (TransferPlan) planRes[0].value();
			if (!TransferReviewDialog.show(plan))
				return;

			@SuppressWarnings("unchecked")
			var execRes = new Res[1];
			App.runWithProgress("Transfer product system", () ->
				execRes[0] = TransferExecutor.execute(plan));
			if (execRes[0] == null || execRes[0].isError()) {
				var error = execRes[0] != null
					? execRes[0].error()
					: "Failed to transfer the product system";
				MsgBox.error("Transfer failed", error);
				return;
			}

			MsgBox.info("Transfer complete",
				"Transferred product system to target database '"
					+ transfer.target().getName() + "'.");
		}

	}

	private TransferTargetDialog(TargetSelection config) {
		super(UI.shell());
		setBlockOnOpen(true);
		this.config = config;
	}


	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Transfer product system");
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
		var filter = UI.text(body, tk, SWT.SEARCH | SWT.CANCEL);
		filter.setMessage("Search product systems");
		createSystemTable(body, filter);

		var bottom = UI.composite(body, tk);
		UI.gridLayout(bottom, 2);
		UI.gridData(bottom, true, false);
		createLinkingCombo(bottom, tk);
		createTargetCombo(bottom, tk);
		updateOk();
	}

	private void createSystemTable(Composite body, Text filter) {
		var table = Tables.createViewer(body, "Product system", "Category");
		Supplier<ProductSystemDescriptor> selection = () -> {
			var selected = Viewers.getFirstSelected(table);
			return selected instanceof ProductSystemDescriptor d ? d : null;
		};

		Tables.bindColumnWidths2(table, 0.6, 0.4);
		table.setLabelProvider(new ProductSystemLabel());
		table.addFilter(new ProductSystemFilter(filter));
		table.addSelectionChangedListener($ -> {
			config.setSystem(selection.get());
			updateOk();
		});
		table.setInput(config.systems());

		if (!config.systems().isEmpty()) {
			var first = config.systems().getFirst();
			config.setSystem(first);
			table.setSelection(new StructuredSelection(first));
		}
		filter.addModifyListener($ -> {
			table.refresh();
			config.setSystem(selection.get());
			updateOk();
		});
	}

	private void createTargetCombo(Composite bottom, FormToolkit tk) {
		var combo = UI.labeledCombo(bottom, tk, "Target database");
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

	private void createLinkingCombo(Composite comp, FormToolkit tk) {
		var combo = UI.labeledCombo(comp, tk, "Provider linking");
		var strats = LinkingStrategy.values();
		var items = new String[strats.length];
		for (int i = 0; i < strats.length; i++) {
			items[i] = switch (strats[i]) {
				case BY_ID -> "Processes by identifier (UUID)";
				case BY_NAME -> "Processes by name and location";
			};
		}

		combo.setItems(items);
		combo.select(0);
		config.setStrategy(strats[0]);
		Controls.onSelect(combo, $ -> {
			int idx = combo.getSelectionIndex();
			config.setStrategy(strats[idx]);
			updateOk();
		});
	}

	private void updateOk() {
		var button = getButton(IDialogConstants.OK_ID);
		if (button == null) return;
		button.setEnabled(config.isComplete());
	}

	private static class ProductSystemFilter extends ViewerFilter {

		private final Text text;

		private ProductSystemFilter(Text text) {
			this.text = text;
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			var filter = text.getText();
			if (filter == null || filter.isBlank())
				return true;
			if (!(element instanceof ProductSystemDescriptor d))
				return false;
			var query = filter.strip().toLowerCase();
			return matches(Labels.name(d), query)
				|| matches(Labels.category(d), query);
		}

		private boolean matches(String value, String query) {
			return value != null && value.toLowerCase().contains(query);
		}
	}

	private static class ProductSystemLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object o, int col) {
			return col == 0 ? Images.get(ModelType.PRODUCT_SYSTEM) : null;
		}

		@Override
		public String getColumnText(Object o, int col) {
			if (!(o instanceof ProductSystemDescriptor d))
				return null;
			return switch (col) {
				case 0 -> Labels.name(d);
				case 1 -> Labels.category(d);
				default -> null;
			};
		}
	}
}
