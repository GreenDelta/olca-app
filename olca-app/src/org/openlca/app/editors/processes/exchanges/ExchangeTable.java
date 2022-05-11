package org.openlca.app.editors.processes.exchanges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.ModelSelector;
import org.openlca.app.components.ModelTransfer;
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
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.io.CategoryPath;
import org.openlca.util.Strings;

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
	public static final String LOCATION = M.Location;
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
		var comp = UI.sectionClient(section, page.toolkit, 1);
		viewer = Tables.createViewer(comp, getColumns());
		label = new ExchangeLabel(editor);
		viewer.setLabelProvider(label);
		bindModifiers();
		ModelTransfer.onDrop(viewer.getTable(), this::add);
		viewer.addFilter(new Filter());
		bindActions(section);
		bindDoubleClick(viewer);
		double x = editor.hasAnyComment("exchanges")
				? 0.7 / 10
				: 0.7 / 9;
			Tables.bindColumnWidths(viewer,
				0.15, 0.15, x, x, x, x, x, x, x, x, x);
		Viewers.sortByLabels(viewer, label, 0, 1, 3, 4, 5, 6, 7, 8);
		Viewers.sortByDouble(viewer, (Exchange e) -> e.amount, 2);
		viewer.getTable().getColumns()[2].setAlignment(SWT.RIGHT);
		viewer.getTable().getColumns()[4].setAlignment(SWT.RIGHT);
	}

	void setInput(Process process) {
		viewer.setInput(process.exchanges);
	}

	private void bindModifiers() {
		if (!editor.isEditable())
			return;
		var ms = new ModifySupport<Exchange>(viewer);
		ms.bind(UNIT, new UnitCell(editor));
		ms.bind(COSTS, new CostCellEditor(viewer, editor));
		ms.bind(PEDIGREE, new DataQualityCellEditor(viewer, editor));
		ms.bind(UNCERTAINTY, new UncertaintyCellEditor(
				viewer.getTable(), editor));
		ms.bind(DESCRIPTION, new CommentEditor(viewer, editor));
		ms.bind(PROVIDER, new ProviderCombo(editor));
		ms.bind(AVOIDED, new AvoidedCheck(editor));
		ms.bind(LOCATION, new LocationCell(
				viewer.getTable(), editor));
		ms.bind("", new CommentDialogModifier<>(
				editor.getComments(), CommentPaths::get));
		bindAmountModifier(ms);
	}

	private void bindAmountModifier(ModifySupport<Exchange> ms) {
		// amount editor with auto-completion support for parameter names
		FormulaCellEditor amountEditor = new FormulaCellEditor(viewer,
				() -> editor.getModel().parameters);
		ms.bind(AMOUNT, amountEditor);
		amountEditor.onEdited((obj, amount) -> {
			if (!(obj instanceof Exchange e))
				return;
			try {
				double value = Double.parseDouble(amount);
				e.formula = null;
				e.amount = value;
			} catch (NumberFormatException ex) {
				e.formula = amount;
				editor.getParameterSupport().evaluate();
			}
			editor.setDirty(true);
			viewer.refresh();
		});
	}

	private void bindActions(Section section) {
		if (!editor.isEditable())
			return;
		var add = Actions.onAdd(
				() -> add(ModelSelector.multiSelect(ModelType.FLOW)));
		Action remove = Actions.onRemove(this::onRemove);
		Action qRef = Actions.create(M.SetAsQuantitativeReference, null, () -> {
			Exchange e = Viewers.getFirstSelected(viewer);
			if (e == null)
				return;
			editor.getModel().quantitativeReference = e;
			page.refreshTables();
			editor.setDirty();
		});
		Action formulaSwitch = new FormulaSwitchAction();
		Action copy = TableClipboard.onCopySelected(viewer, this::toClipboard);
		Action paste = TableClipboard.onPaste(viewer, this::onPaste);
		CommentAction.bindTo(section, "exchanges",
				editor.getComments(), add, remove, formulaSwitch);
		Tables.onDeletePressed(viewer, e -> onRemove());
		Action openFlow = Actions.create(
				M.OpenFlow, Images.descriptor(ModelType.FLOW), () -> {
					Exchange e = Viewers.getFirstSelected(viewer);
					if (e == null || e.flow == null)
						return;
					App.open(e.flow);
				});
		Action openProvider = Actions.create(
				M.OpenProvider, Images.descriptor(ModelType.PROCESS), () -> {
					Exchange e = Viewers.getFirstSelected(viewer);
					if (e == null || e.defaultProviderId == 0L)
						return;
					ProcessDao dao = new ProcessDao(Database.get());
					ProcessDescriptor d = dao.getDescriptor(e.defaultProviderId);
					if (d != null) {
						App.open(d);
					}
				});
		Actions.bind(viewer, add, remove, qRef,
				copy, paste, openFlow, openProvider);
	}

	private void bindDoubleClick(TableViewer table) {
		Tables.onDoubleClick(table, e -> {
			TableItem item = Tables.getItem(table, e);
			if (item == null) {
				add(ModelSelector.multiSelect(ModelType.FLOW));
				return;
			}
			Exchange exchange = Viewers.getFirstSelected(table);
			if (exchange != null && exchange.flow != null)
				App.open(exchange.flow);
		});
	}

	private String[] getColumns() {
		List<String> columns = new ArrayList<>(
				Arrays.asList(FLOW, CATEGORY, AMOUNT, UNIT,
						COSTS, UNCERTAINTY, AVOIDED, PROVIDER,
						PEDIGREE, LOCATION, DESCRIPTION));
		if (editor.hasAnyComment("exchanges")) {
			columns.add(COMMENT);
		}
		return columns.toArray(new String[0]);
	}

	private void onRemove() {
		Process process = editor.getModel();
		List<Exchange> selection = Viewers.getAllSelected(viewer);
		Boolean b = App.exec(
				"Check usage of exchanges",
				() -> Exchanges.canRemove(process, selection));
		if (b == null || !b)
			return;
		selection.forEach(e -> process.exchanges.remove(e));
		viewer.setInput(process.exchanges);
		editor.setDirty(true);
		editor.emitEvent(ProcessEditor.EXCHANGES_CHANGED);
	}

	private void add(List<? extends Descriptor> descriptors) {
		if (descriptors.isEmpty())
			return;
		Process process = editor.getModel();
		boolean added = false;
		for (var d : descriptors) {
			long flowId = Exchanges.refFlowID(d);
			if (flowId < 1)
				continue;
			FlowDao dao = new FlowDao(Database.get());
			Flow flow = dao.getForId(flowId);
			if (!canAdd(flow))
				continue;
			var e = forInputs
					? process.input(flow, 1)
					: process.output(flow, 1);
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
		editor.emitEvent(ProcessEditor.EXCHANGES_CHANGED);
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
		editor.emitEvent(ProcessEditor.EXCHANGES_CHANGED);
		editor.getParameterSupport().evaluate();
	}

	private String toClipboard(TableItem item, int col) {
		if (item == null)
			return "";
		Object data = item.getData();
		if (!(data instanceof Exchange e))
			return TableClipboard.text(item, col);
		switch (col) {
		case 1:
			if (e.flow != null && e.flow.category != null)
				return CategoryPath.getFull(e.flow.category);
			else
				return "";
		case 2:
			if (label.showFormulas
					&& Strings.notEmpty(e.formula))
				return e.formula;
			else
				return Double.toString(e.amount);
		case 4:
			if (e.costs == null || e.currency == null)
				return "";
			if (label.showFormulas
					&& Strings.notEmpty(e.costFormula))
				return e.costFormula + " " + e.currency.code;
			else
				return e.costs + " " + e.currency.code;
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
			if (!(obj instanceof Exchange e))
				return false;
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
