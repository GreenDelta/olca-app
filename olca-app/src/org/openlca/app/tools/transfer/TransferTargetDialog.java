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
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.openlca.app.App;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.commons.Res;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;
import org.openlca.io.olca.systransfer.MatchingStrategy;
import org.openlca.io.olca.systransfer.TransferPlan;

public class TransferTargetDialog extends FormDialog {

	private final TargetSelection config;
	private org.eclipse.jface.viewers.TableViewer strategyTable;
	private ImageHyperlink upLink;
	private ImageHyperlink downLink;
	private ImageHyperlink removeLink;

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
			var planRes = new Res[1];
			App.runWithProgress("Prepare transfer", () ->
				planRes[0] = TransferPlan.createFrom(transfer.config()));
			if (planRes[0] == null || planRes[0].isError()) {
				var error = planRes[0] != null
					? planRes[0].error()
					: "Failed to prepare the transfer";
				MsgBox.error("Cannot transfer a product system", error);
				return;
			}

			var plan = (TransferPlan) planRes[0].value();
			TransferPlanEditor.open(plan);
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
		createTargetCombo(body, tk);
		createMatchingTable(body, tk);
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

	private void createTargetCombo(Composite parent, FormToolkit tk) {
		var combo = UI.labeledCombo(parent, tk, "Target database");
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

	private void createMatchingTable(Composite parent, FormToolkit tk) {
		var comp = UI.composite(parent, tk);
		UI.gridLayout(comp, 2);
		UI.gridData(comp, true, true);

		strategyTable = Tables.createViewer(comp, "Provider matching");
		UI.gridData(strategyTable.getControl(), true, true);
		strategyTable.setLabelProvider(new StrategyLabel());
		strategyTable.setInput(config.strategies());
		strategyTable.addSelectionChangedListener($ -> updateStrategyButtons());

		var buttons = UI.composite(comp, tk);
		UI.gridLayout(buttons, 1);
		UI.gridData(buttons, false, false);

		upLink = createIconLink(buttons, tk,
			Icon.UP,
			Icon.UP_DISABLED,
			"Move strategy up",
			() -> moveSelectedStrategy(-1));
		downLink = createIconLink(buttons, tk,
			Icon.DOWN,
			Icon.DOWN_DISABLED,
			"Move strategy down",
			() -> moveSelectedStrategy(1));
		removeLink = createIconLink(buttons, tk,
			Icon.DELETE,
			Icon.DELETE_DISABLED,
			"Remove strategy",
			this::removeSelectedStrategy);

		if (!config.strategies().isEmpty()) {
			strategyTable.setSelection(
				new StructuredSelection(config.strategies().getFirst()),
				true);
		}
		updateStrategyButtons();
	}

	private ImageHyperlink createIconLink(
		Composite parent,
		FormToolkit tk,
		Icon enabledIcon,
		Icon disabledIcon,
		String toolTip,
		Runnable action
	) {
		var link = UI.imageHyperlink(parent, tk, SWT.TOP);
		link.setImage(disabledIcon.get());
		link.setToolTipText(toolTip);
		Controls.onClick(link, $ -> action.run());
		link.setData("enabledIcon", enabledIcon);
		link.setData("disabledIcon", disabledIcon);
		return link;
	}

	private void moveSelectedStrategy(int delta) {
		var strategy = selectedStrategy();
		if (strategy == null)
			return;
		config.moveStrategy(strategy, delta);
		strategyTable.refresh();
		strategyTable.setSelection(new StructuredSelection(strategy), true);
		updateStrategyButtons();
		updateOk();
	}

	private void removeSelectedStrategy() {
		var strategy = selectedStrategy();
		if (strategy == null)
			return;
		int index = config.strategies().indexOf(strategy);
		config.removeStrategy(strategy);
		strategyTable.refresh();
		if (!config.strategies().isEmpty()) {
			int nextIndex = Math.min(index, config.strategies().size() - 1);
			strategyTable.setSelection(
				new StructuredSelection(config.strategies().get(nextIndex)),
				true);
		}
		updateStrategyButtons();
		updateOk();
	}

	private MatchingStrategy selectedStrategy() {
		var selected = Viewers.getFirstSelected(strategyTable);
		return selected instanceof MatchingStrategy strategy ? strategy : null;
	}

	private void updateStrategyButtons() {
		updateLink(upLink, config.canMoveUp(selectedStrategy()));
		updateLink(downLink, config.canMoveDown(selectedStrategy()));
		updateLink(removeLink, selectedStrategy() != null);
	}

	private void updateLink(ImageHyperlink link, boolean enabled) {
		if (link == null || link.isDisposed())
			return;
		var enabledIcon = (Icon) link.getData("enabledIcon");
		var disabledIcon = (Icon) link.getData("disabledIcon");
		link.setEnabled(enabled);
		link.setImage((enabled ? enabledIcon : disabledIcon).get());
	}

	private void updateOk() {
		var button = getButton(IDialogConstants.OK_ID);
		if (button == null) return;
		button.setEnabled(config.isComplete());
	}

	private static class StrategyLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof MatchingStrategy strategy))
				return null;
			return strategyLabel(strategy);
		}
	}

	private static String strategyLabel(MatchingStrategy strategy) {
		return switch (strategy) {
			case BY_ID -> "Match providers by their IDs";
			case BY_NAME -> "Match providers by their name and location";
			case ANY -> "Match providers by flows";
		};
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
