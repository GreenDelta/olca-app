package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.FlowDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;

/**
 * The table for the display and editing of inputs or outputs of process
 * exchanges. Avoided products are inputs that are shown on the output site in
 * this table.
 * 
 */
class ExchangeTable {

	TableViewer viewer;

	private final boolean forInputs;
	private final ProcessEditor editor;
	private final ProcessExchangePage page;

	private static final String FLOW = M.Flow;
	private static final String CATEGORY = M.Category;
	private static final String AMOUNT = M.Amount;
	private static final String UNIT = M.Unit;
	private static final String COSTS = M.CostsRevenues;
	private static final String PEDIGREE = M.DataQualityEntry;
	private static final String PROVIDER = M.DefaultProvider;
	private static final String UNCERTAINTY = M.Uncertainty;
	private static final String DESCRIPTION = M.Description;
	private static final String COMMENT = "";
	private final String AVOIDED;

	private ExchangeLabel label;

	public static ExchangeTable forInputs(Section section, ProcessExchangePage page) {
		ExchangeTable table = new ExchangeTable(true, page);
		table.render(section);
		return table;
	}

	public static ExchangeTable forOutputs(Section section,
			ProcessExchangePage page) {
		ExchangeTable table = new ExchangeTable(false, page);
		table.render(section);
		return table;
	}

	private ExchangeTable(boolean forInputs, ProcessExchangePage page) {
		this.forInputs = forInputs;
		this.page = page;
		this.editor = page.editor;
		this.AVOIDED = forInputs ? M.AvoidedWaste : M.AvoidedProduct;
		editor.getParameterSupport().afterEvaluation(() -> viewer.refresh());
	}

	private void render(Section section) {
		Composite composite = UI.sectionClient(section, page.toolkit, 1);
		viewer = Tables.createViewer(composite, getColumns());
		label = new ExchangeLabel(editor);
		viewer.setLabelProvider(label);
		bindModifiers();
		Tables.addDropSupport(viewer, this::add);
		viewer.addFilter(new Filter());
		bindActions(section);
		bindDoubleClick(viewer);
		if (editor.hasAnyComment("exchanges")) {
			Tables.bindColumnWidths(viewer, 0.2, 0.15, 0.1, 0.08, 0.08, 0.08, 0.08, 0.07, 0.07, 0.06);
		} else {
			Tables.bindColumnWidths(viewer, 0.2, 0.15, 0.1, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.07);
		}
		Viewers.sortByLabels(viewer, label, 0, 1, 3, 4, 5, 6, 7, 8);
		Viewers.sortByDouble(viewer, (Exchange e) -> e.amount, 2);
		viewer.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
		viewer.getTable().getColumns()[4].setAlignment(SWT.RIGHT);
	}

	void setInput(Process process) {
		viewer.setInput(process.getExchanges());
	}

	private void bindModifiers() {
		ModifySupport<Exchange> ms = new ModifySupport<>(viewer);
		ms.bind(AMOUNT, new AmountModifier());
		ms.bind(UNIT, new UnitCell(editor));
		ms.bind(COSTS, new CostCellEditor(viewer, editor));
		ms.bind(PEDIGREE, new DataQualityCellEditor(viewer, editor));
		ms.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable(), editor));
		ms.bind(DESCRIPTION, new CommentEditor(viewer, editor));
		ms.bind(PROVIDER, new ProviderCombo(editor));
		ms.bind(AVOIDED, new AvoidedCheck(editor));
		ms.bind("", new CommentDialogModifier<Exchange>(editor.getComments(), CommentPaths::get));
	}

	private void bindActions(Section section) {
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Action qRef = Actions.create(M.SetAsQuantitativeReference, null, () -> {
			Exchange e = Viewers.getFirstSelected(viewer);
			if (e == null)
				return;
			editor.getModel().setQuantitativeReference(e);
			page.refreshTables();
			editor.setDirty(true);
		});
		Action formulaSwitch = new FormulaSwitchAction();
		Action copy = TableClipboard.onCopy(viewer, Clipboard::converter);
		Action paste = TableClipboard.onPaste(viewer, this::onPaste);
		CommentAction.bindTo(section, "exchanges", editor.getComments(), add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove, qRef, copy, paste);
		Tables.onDeletePressed(viewer, e -> onRemove());
	}

	private void bindDoubleClick(TableViewer table) {
		Tables.onDoubleClick(table, e -> {
			TableItem item = Tables.getItem(table, e);
			if (item == null) {
				onAdd();
				return;
			}
			Exchange exchange = Viewers.getFirstSelected(table);
			if (exchange != null && exchange.flow != null)
				App.openEditor(exchange.flow);
		});
	}

	private String[] getColumns() {
		List<String> columns = new ArrayList<>(
				Arrays.asList(FLOW, CATEGORY, AMOUNT, UNIT,
						COSTS, UNCERTAINTY, AVOIDED, PROVIDER,
						PEDIGREE, DESCRIPTION));
		if (editor.hasAnyComment("exchanges")) {
			columns.add(COMMENT);
		}
		return columns.toArray(new String[columns.size()]);
	}

	private void onAdd() {
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect(ModelType.FLOW);
		if (descriptors != null) {
			add(Arrays.asList(descriptors));
		}
	}

	private void onRemove() {
		Process process = editor.getModel();
		List<Exchange> selection = Viewers.getAllSelected(viewer);
		if (!Exchanges.canRemove(process, selection))
			return;
		selection.forEach(e -> process.getExchanges().remove(e));
		viewer.setInput(process.getExchanges());
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
	}

	private void add(List<BaseDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		Process process = editor.getModel();
		for (BaseDescriptor descriptor : descriptors) {
			if (!(descriptor instanceof FlowDescriptor))
				continue;
			FlowDao flowDao = new FlowDao(Database.get());
			Flow flow = flowDao.getForId(descriptor.getId());
			if (!canAdd(flow)) {
				continue;
			}
			Exchange e = process.exchange(flow);
			e.isInput = forInputs;
		}
		viewer.setInput(process.getExchanges());
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
	}

	private void onPaste(String text) {
		List<Exchange> exchanges = new ArrayList<>();
		App.runWithProgress("Paste exchanges ...",
				() -> exchanges.addAll(Clipboard.read(text, forInputs)));
		if (exchanges.isEmpty())
			return;
		Process process = editor.getModel();
		for (Exchange e : exchanges) {
			e.internalId = ++process.lastInternalId;
			process.getExchanges().add(e);
		}
		viewer.setInput(process.getExchanges());
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
	}

	private boolean canAdd(Flow flow) {
		if (flow == null)
			return false;
		if (forInputs && flow.getFlowType() == FlowType.WASTE_FLOW)
			for (Exchange ex : editor.getModel().getExchanges())
				if (ex.isInput && ex.flow.equals(flow))
					return false;
		if (!forInputs && flow.getFlowType() == FlowType.PRODUCT_FLOW)
			for (Exchange ex : editor.getModel().getExchanges())
				if (!ex.isInput && ex.flow.equals(flow))
					return false;
		return true;
	}

	private class AmountModifier extends TextCellModifier<Exchange> {

		@Override
		protected String getText(Exchange e) {
			if (e.amountFormula == null)
				return Double.toString(e.amount);
			return e.amountFormula;
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			try {
				double value = Double.parseDouble(text);
				exchange.amountFormula = null;
				exchange.amount = value;
				editor.setDirty(true);
			} catch (NumberFormatException e) {
				try {
					exchange.amountFormula = text;
					editor.setDirty(true);
					editor.getParameterSupport().evaluate();
				} catch (Exception ex) {
					Error.showBox(M.InvalidFormula, text + " "
							+ M.IsInvalidFormula);
				}
			}
		}
	}

	private class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parent, Object obj) {
			if (!(obj instanceof Exchange))
				return false;
			Exchange e = (Exchange) obj;
			if (e.isAvoided)
				return e.isInput != forInputs;
			else
				return e.isInput == forInputs;
		}
	}

	private class FormulaSwitchAction extends Action {

		private boolean showFormulas = true;

		public FormulaSwitchAction() {
			setImageDescriptor(Icon.NUMBER.descriptor());
			setText(M.ShowValues);
		}

		@Override
		public void run() {
			showFormulas = !showFormulas;
			if (showFormulas) {
				setImageDescriptor(Icon.NUMBER.descriptor());
				setText(M.ShowValues);
			} else {
				setImageDescriptor(Icon.FORMULA.descriptor());
				setText(M.ShowFormulas);
			}
			label.showFormulas = showFormulas;
			viewer.refresh();
		}
	}
}
