package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		Tables.onDrop(viewer, this::add);
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
		viewer.setInput(process.exchanges);
	}

	private void bindModifiers() {
		ModifySupport<Exchange> ms = new ModifySupport<>(viewer);
		ms.bind(UNIT, new UnitCell(editor));
		ms.bind(COSTS, new CostCellEditor(viewer, editor));
		ms.bind(PEDIGREE, new DataQualityCellEditor(viewer, editor));
		ms.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable(), editor));
		ms.bind(DESCRIPTION, new CommentEditor(viewer, editor));
		ms.bind(PROVIDER, new ProviderCombo(editor));
		ms.bind(AVOIDED, new AvoidedCheck(editor));
		ms.bind("", new CommentDialogModifier<Exchange>(
				editor.getComments(), CommentPaths::get));
		bindAmountModifier(ms);
	}

	private void bindAmountModifier(ModifySupport<Exchange> ms) {
		// amount editor with auto-completion support for parameter names
		FormulaCellEditor amountEditor = new FormulaCellEditor(viewer,
				() -> editor.getModel().parameters);
		ms.bind(AMOUNT, amountEditor);
		amountEditor.onEdited((obj, amount) -> {
			if (!(obj instanceof Exchange))
				return;
			Exchange e = (Exchange) obj;
			try {
				double value = Double.parseDouble(amount);
				e.amountFormula = null;
				e.amount = value;
			} catch (NumberFormatException ex) {
				e.amountFormula = amount;
				editor.getParameterSupport().evaluate();
			}
			editor.setDirty(true);
			viewer.refresh();
		});
	}

	private void bindActions(Section section) {
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Action qRef = Actions.create(M.SetAsQuantitativeReference, null, () -> {
			Exchange e = Viewers.getFirstSelected(viewer);
			if (e == null)
				return;
			editor.getModel().quantitativeReference = e;
			page.refreshTables();
			editor.setDirty(true);
		});
		Action formulaSwitch = new FormulaSwitchAction();
		Action copy = TableClipboard.onCopy(viewer, this::toClipboard);
		Action paste = TableClipboard.onPaste(viewer, this::onPaste);
		CommentAction.bindTo(section, "exchanges",
				editor.getComments(), add, remove, formulaSwitch);
		Tables.onDeletePressed(viewer, e -> onRemove());
		Action openFlow = Actions.create(
				M.OpenFlow, Images.descriptor(ModelType.FLOW), () -> {
					Exchange e = Viewers.getFirstSelected(viewer);
					if (e == null || e.flow == null)
						return;
					App.openEditor(e.flow);
				});
		Action openProvider = Actions.create(
				M.OpenProvider, Images.descriptor(ModelType.PROCESS), () -> {
					Exchange e = Viewers.getFirstSelected(viewer);
					if (e == null || e.defaultProviderId == 0L)
						return;
					ProcessDao dao = new ProcessDao(Database.get());
					ProcessDescriptor d = dao.getDescriptor(e.defaultProviderId);
					if (d != null) {
						App.openEditor(d);
					}
				});
		Actions.bind(viewer, add, remove, qRef,
				copy, paste, openFlow, openProvider);
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
		selection.forEach(e -> process.exchanges.remove(e));
		viewer.setInput(process.exchanges);
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
	}

	private void add(List<BaseDescriptor> descriptors) {
		if (descriptors.isEmpty())
			return;

		Process process = editor.getModel();
		boolean added = false;
		for (BaseDescriptor d : descriptors) {
			long flowId = -1;
			if (d.type == ModelType.FLOW) {
				flowId = d.id;
			} else if (d.type == ModelType.PROCESS) {

				// query the reference flow of the process
				String sql = "select e.f_flow from tbl_processes p "
						+ "inner join tbl_exchanges e on "
						+ "p.f_quantitative_reference = e.id "
						+ "where p.id = " + d.id;
				try {
					AtomicLong id = new AtomicLong(flowId);
					NativeSql.on(Database.get()).query(sql, r -> {
						id.set(r.getLong(1));
						return false;
					});
					flowId = id.get();
				} catch (Exception e) {
					Logger log = LoggerFactory.getLogger(getClass());
					log.error("Failed to query ref. flow: " + sql, e);
				}
			}

			if (flowId < 1)
				continue;
			FlowDao dao = new FlowDao(Database.get());
			Flow flow = dao.getForId(flowId);
			if (!canAdd(flow))
				continue;
			Exchange e = process.exchange(flow);
			e.isInput = forInputs;
			if (d.type == ModelType.PROCESS
					&& Exchanges.canHaveProvider(e)) {
				e.defaultProviderId = d.id;
			}
			added = true;
		}

		if (!added)
			return;
		viewer.setInput(process.exchanges);
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
			process.exchanges.add(e);
		}
		viewer.setInput(process.exchanges);
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
		editor.getParameterSupport().evaluate();
	}

	private String toClipboard(TableItem item, int col) {
		if (item == null)
			return "";
		Object data = item.getData();
		if (!(data instanceof Exchange))
			return TableClipboard.text(item, col);
		Exchange e = (Exchange) data;
		switch (col) {
		case 1:
			if (e.flow != null && e.flow.category != null)
				return CategoryPath.getFull(e.flow.category);
			else
				return "";
		case 2:
			if (label.showFormulas
					&& Strings.notEmpty(e.amountFormula))
				return e.amountFormula;
			else
				return Double.toString(e.amount);
		case 4:
			if (e.costs == null || e.currency == null)
				return "";
			if (label.showFormulas
					&& Strings.notEmpty(e.costFormula))
				return e.costFormula + " " + e.currency.code;
			else
				return e.costs.toString() + " " + e.currency.code;
		case 6:
			return e.isAvoided ? "TRUE" : "";
		default:
			return TableClipboard.text(item, col);
		}
	}

	private boolean canAdd(Flow flow) {
		if (flow == null)
			return false;
		if (forInputs && flow.flowType == FlowType.WASTE_FLOW)
			for (Exchange ex : editor.getModel().exchanges)
				if (ex.isInput && Objects.equals(ex.flow, flow))
					return false;
		if (!forInputs && flow.flowType == FlowType.PRODUCT_FLOW)
			for (Exchange ex : editor.getModel().exchanges)
				if (!ex.isInput && Objects.equals(ex.flow, flow))
					return false;
		return true;
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
