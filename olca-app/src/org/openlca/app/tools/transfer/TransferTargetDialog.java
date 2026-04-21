package org.openlca.app.tools.transfer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProductSystemDescriptor;

public class TransferTargetDialog extends FormDialog {

	private final List<ProductSystemDescriptor> productSystems = new ArrayList<>();
	private final List<DatabaseConfig> targetDatabases = new ArrayList<>();

	private IDatabase sourceDb;
	private TableViewer productSystemTable;
	private Combo providerLinkingCombo;
	private Combo targetDatabaseCombo;
	private TransferTargetSelection selection;

	public TransferTargetDialog() {
		super(UI.shell());
		setBlockOnOpen(true);
	}

	@Override
	public int open() {
		sourceDb = Database.get();
		if (sourceDb == null) {
			MsgBox.error("No database opened", "You need to open a source database first.");
			return CANCEL;
		}
		if (!loadProductSystems())
			return CANCEL;
		if (productSystems.isEmpty()) {
			MsgBox.info("No product systems available",
				"The currently opened database does not contain any product systems.");
			return CANCEL;
		}
		loadTargetDatabases();
		if (targetDatabases.isEmpty()) {
			MsgBox.info("No target databases available",
				"No other database was found in the openLCA workspace.");
			return CANCEL;
		}
		selection = null;
		return super.open();
	}

	public TransferTargetSelection selection() {
		return selection;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Transfer product system");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(820, 560);
	}

	@Override
	protected void createFormContent(IManagedForm form) {
		var tk = form.getToolkit();
		var body = UI.dialogBody(form.getForm(), tk);
		UI.gridLayout(body, 1);

		var filterText = UI.text(body, tk, SWT.SEARCH | SWT.ICON_SEARCH | SWT.CANCEL);
		filterText.setMessage("Search product systems");

		productSystemTable = Tables.createViewer(body,
			"Product system",
			"Category");
		Tables.bindColumnWidths2(productSystemTable, 0.6, 0.4);
		var label = new ProductSystemLabel();
		productSystemTable.setLabelProvider(label);
		Viewers.sortByLabels(productSystemTable, label, 0, 1, 2);
		productSystemTable.addFilter(new ProductSystemFilter(productSystemTable, filterText));
		productSystemTable.addSelectionChangedListener($ -> updateOkButton());
		productSystemTable.setInput(productSystems);
		if (!productSystems.isEmpty()) {
			productSystemTable.setSelection(new StructuredSelection(productSystems.getFirst()));
		}

		var bottom = UI.composite(body, tk);
		UI.gridLayout(bottom, 2);
		UI.gridData(bottom, true, false);

		providerLinkingCombo = UI.labeledCombo(bottom, tk, "Provider linking");
		providerLinkingCombo.setItems(strategyLabels());
		providerLinkingCombo.select(0);
		providerLinkingCombo.addListener(SWT.Selection, $ -> updateOkButton());

		targetDatabaseCombo = UI.labeledCombo(bottom, tk, "Target database");
		targetDatabaseCombo.setItems(targetDatabaseLabels());
		targetDatabaseCombo.select(0);
		targetDatabaseCombo.addListener(SWT.Selection, $ -> updateOkButton());
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
		selection = new TransferTargetSelection(
			productSystem,
			targetDatabase,
			providerLinkingStrategy);
		super.okPressed();
	}

	private boolean loadProductSystems() {
		productSystems.clear();
		try {
			for (var descriptor : sourceDb.getDescriptors(ProductSystem.class)) {
				if (descriptor instanceof ProductSystemDescriptor productSystem) {
					productSystems.add(productSystem);
				}
			}
			productSystems.sort(Comparator.comparing(
				d -> Labels.name(d) != null ? Labels.name(d) : "",
				String.CASE_INSENSITIVE_ORDER));
			return true;
		} catch (Exception e) {
			ErrorReporter.on("Failed to load product systems", e);
			return false;
		}
	}

	private void loadTargetDatabases() {
		targetDatabases.clear();
		for (var config : Database.getConfigurations().getAll()) {
			if (!Database.isActive(config)) {
				targetDatabases.add(config);
			}
		}
		targetDatabases.sort(Comparator.comparing(DatabaseConfig::name,
			String.CASE_INSENSITIVE_ORDER));
	}

	private String[] strategyLabels() {
		var strategies = ProviderLinkingStrategy.values();
		var labels = new String[strategies.length];
		for (int i = 0; i < strategies.length; i++) {
			labels[i] = strategies[i].label();
		}
		return labels;
	}

	private String[] targetDatabaseLabels() {
		var labels = new String[targetDatabases.size()];
		for (int i = 0; i < targetDatabases.size(); i++) {
			labels[i] = targetDatabases.get(i).name();
		}
		return labels;
	}

	private ProductSystemDescriptor selectedProductSystem() {
		var selected = Viewers.getFirstSelected(productSystemTable);
		return selected instanceof ProductSystemDescriptor productSystem
			? productSystem
			: null;
	}

	private ProviderLinkingStrategy selectedStrategy() {
		var index = providerLinkingCombo != null
			? providerLinkingCombo.getSelectionIndex()
			: -1;
		if (index < 0)
			return null;
		var strategies = ProviderLinkingStrategy.values();
		return index < strategies.length
			? strategies[index]
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
		if (button == null)
			return;
		button.setEnabled(selectedProductSystem() != null
			&& selectedStrategy() != null
			&& selectedTargetDatabase() != null);
	}

	private static class ProductSystemFilter extends ViewerFilter {

		private final TableViewer table;
		private final Text filterText;

		private ProductSystemFilter(TableViewer table, Text filterText) {
			this.table = table;
			this.filterText = filterText;
			this.filterText.addModifyListener($ -> this.table.refresh());
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			var filter = filterText.getText();
			if (filter == null || filter.isBlank())
				return true;
			if (!(element instanceof ProductSystemDescriptor productSystem))
				return false;
			var query = filter.strip().toLowerCase();
			return matches(Labels.name(productSystem), query)
				|| matches(productSystem.refId, query)
				|| matches(Labels.category(productSystem), query);
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
