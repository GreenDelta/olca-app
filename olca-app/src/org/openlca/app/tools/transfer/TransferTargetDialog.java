package org.openlca.app.tools.transfer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class TransferTargetDialog extends FormDialog {

	private final Config config;

	private TableViewer sysTable;
	private Combo targetDatabaseCombo;

	public static void show() {

		var res = Config.load();
		if (res.isError()) {
			MsgBox.error("Cannot transfer a product system", res.error());
			return;
		}

		var config = res.value();
		var dialog = new TransferTargetDialog(config);
		if (dialog.open() != OK || !config.isComplete())
			return;

	}

	private TransferTargetDialog(Config config) {
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

		var filter = UI.text(
			body, tk, SWT.SEARCH | SWT.ICON_SEARCH | SWT.CANCEL);
		filter.setMessage("Search product systems");

		sysTable = Tables.createViewer(
			body, "Product system", "Category");
		Tables.bindColumnWidths2(sysTable, 0.6, 0.4);
		var label = new ProductSystemLabel();
		sysTable.setLabelProvider(label);
		Viewers.sortByLabels(sysTable, label, 0, 1, 2);
		sysTable.addFilter(new ProductSystemFilter(sysTable, filter));
		sysTable.addSelectionChangedListener($ -> updateOkButton());
		sysTable.setInput(config.systems());
		if (!config.systems().isEmpty()) {
			sysTable.setSelection(
				new StructuredSelection(config.systems().getFirst()));
		}

		var bottom = UI.composite(body, tk);
		UI.gridLayout(bottom, 2);
		UI.gridData(bottom, true, false);

		createLinkingCombo(bottom, tk);

		targetDatabaseCombo = UI.labeledCombo(bottom, tk, "Target database");
		targetDatabaseCombo.setItems(targetDatabaseLabels());
		targetDatabaseCombo.select(0);
		targetDatabaseCombo.addListener(SWT.Selection, $ -> updateOkButton());
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
			updateOkButton();
		});
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		updateOkButton();
	}

	@Override
	protected void okPressed() {
		var productSystem = selectedProductSystem();
		var targetDatabase = selectedTargetDatabase();
		var providerLinkingStrategy = selectedStrategy();
		if (productSystem == null || targetDatabase == null || providerLinkingStrategy == null)
			return;
		super.okPressed();
	}

	private String[] targetDatabaseLabels() {
		var labels = new String[targetDatabases.size()];
		for (int i = 0; i < targetDatabases.size(); i++) {
			labels[i] = targetDatabases.get(i).name();
		}
		return labels;
	}

	private ProductSystemDescriptor selectedProductSystem() {
		var selected = Viewers.getFirstSelected(sysTable);
		return selected instanceof ProductSystemDescriptor productSystem
			? productSystem
			: null;
	}


	private DatabaseConfig selectedTargetDatabase() {
		var index = targetDatabaseCombo != null
			? targetDatabaseCombo.getSelectionIndex()
			: -1;
		return index >= 0 && index < targetDatabases.size()
			? targetDatabases.get(index)
			: null;
	}

	private void updateOkButton() {
		var button = getButton(IDialogConstants.OK_ID);
		if (button == null) return;
		button.setEnabled(config.isComplete());
	}

	private static class ProductSystemFilter extends ViewerFilter {

		private final TableViewer table;
		private final Text text;

		private ProductSystemFilter(TableViewer table, Text text) {
			this.table = table;
			this.text = text;
			this.text.addModifyListener($ -> this.table.refresh());
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
