package org.openlca.app.editors.processes.exchanges;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.CheckBoxCellModifier;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.EntityCache;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.util.Strings;

/**
 * The table for the display and editing of inputs or outputs of process
 * exchanges. Avoided products are inputs that are shown on the output site in
 * this table.
 * 
 */
class ExchangeTable {

	private final boolean forInputs;
	private final ProcessEditor editor;

	private IDatabase database = Database.get();
	private EntityCache cache = Cache.getEntityCache();

	private final String FLOW = M.Flow;
	private final String CATEGORY = M.Category;
	private final String AMOUNT = M.Amount;
	private final String UNIT = M.Unit;
	private final String COSTS;
	private final String PEDIGREE = "#Data quality entry";
	private final String DEFAULT_PROVIDER = M.DefaultProvider;
	private final String UNCERTAINTY = M.Uncertainty;
	private final String DESCRIPTION = M.Description;
	private final String AVOIDED_PRODUCT = M.AvoidedProduct;

	private TableViewer viewer;
	private ExchangeLabel label;

	public static ExchangeTable forInputs(Section section, FormToolkit toolkit,
			ProcessEditor editor) {
		ExchangeTable table = new ExchangeTable(true, editor);
		table.render(section, toolkit);
		return table;
	}

	public static ExchangeTable forOutputs(Section section,
			FormToolkit toolkit, ProcessEditor editor) {
		ExchangeTable table = new ExchangeTable(false, editor);
		table.render(section, toolkit);
		return table;
	}

	private ExchangeTable(boolean forInputs, ProcessEditor editor) {
		this.forInputs = forInputs;
		this.COSTS = forInputs ? M.Costs : M.CostsRevenues;
		this.editor = editor;
		editor.getParameterSupport().afterEvaluation(() -> viewer.refresh());
	}

	private void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		viewer = Tables.createViewer(composite, getColumns());
		label = new ExchangeLabel(editor, forInputs);
		viewer.setLabelProvider(label.asColumnLabel());
		bindModifiers();
		Tables.addDropSupport(viewer, this::add);
		viewer.addFilter(new Filter());
		bindActions(section, viewer);
		bindDoubleClick(viewer);
		Tables.bindColumnWidths(viewer, 0.2, 0.15, 0.1, 0.09, 0.09, 0.09, 0.09,
				0.09, 0.05);
		Viewers.sortByLabels(viewer, label, 0, 1, 3, 4, 5, 6, 7, 8);
		Viewers.sortByDouble(viewer, (Exchange e) -> e.getAmountValue(), 2);
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
		ms.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable(),
				editor));
		ms.bind(DESCRIPTION, new CommentEditor(viewer, editor));
		if (forInputs)
			ms.bind(DEFAULT_PROVIDER, new ProviderModifier());
		if (!forInputs)
			ms.bind(AVOIDED_PRODUCT, new AvoidedProductModifier());
	}

	private void bindActions(Section section, final TableViewer viewer) {
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Action formulaSwitch = new FormulaSwitchAction();
		Action clipboard = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove, clipboard);
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
			if (exchange != null && exchange.getFlow() != null)
				App.openEditor(exchange.getFlow());
		});
	}

	private String[] getColumns() {
		if (forInputs)
			return new String[] { FLOW, CATEGORY, AMOUNT, UNIT, COSTS,
					UNCERTAINTY, DEFAULT_PROVIDER, PEDIGREE, DESCRIPTION };
		else
			return new String[] { FLOW, CATEGORY, AMOUNT, UNIT, COSTS,
					UNCERTAINTY, AVOIDED_PRODUCT, PEDIGREE, DESCRIPTION };
	}

	private void onAdd() {
		BaseDescriptor[] descriptors = ModelSelectionDialog
				.multiSelect(ModelType.FLOW);
		if (descriptors != null)
			add(Arrays.asList(descriptors));
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
			Exchange e = new Exchange();
			FlowDao flowDao = new FlowDao(Database.get());
			Flow flow = flowDao.getForId(descriptor.getId());
			e.setFlow(flow);
			e.setFlowPropertyFactor(flow.getReferenceFactor());
			Unit unit = getUnit(flow.getReferenceFactor());
			e.setUnit(unit);
			e.setAmountValue(1.0);
			e.setInput(forInputs);
			process.getExchanges().add(e);
		}
		viewer.setInput(process.getExchanges());
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
	}

	private Unit getUnit(FlowPropertyFactor factor) {
		if (factor == null)
			return null;
		if (factor.getFlowProperty() == null)
			return null;
		if (factor.getFlowProperty().getUnitGroup() == null)
			return null;
		return factor.getFlowProperty().getUnitGroup().getReferenceUnit();
	}

	private class AmountModifier extends TextCellModifier<Exchange> {

		@Override
		protected String getText(Exchange e) {
			if (e.getAmountFormula() == null)
				return Double.toString(e.getAmountValue());
			return e.getAmountFormula();
		}

		@Override
		protected void setText(Exchange exchange, String text) {
			try {
				double value = Double.parseDouble(text);
				exchange.setAmountFormula(null);
				exchange.setAmountValue(value);
				editor.setDirty(true);
			} catch (NumberFormatException e) {
				try {
					exchange.setAmountFormula(text);
					editor.setDirty(true);
					editor.getParameterSupport().evaluate();
				} catch (Exception ex) {
					Error.showBox(M.InvalidFormula, text + " "
							+ M.IsInvalidFormula);
				}
			}
		}
	}

	private class ProviderModifier extends
			ComboBoxCellModifier<Exchange, ProcessDescriptor> {

		@Override
		public boolean canModify(Exchange e) {
			return e.isInput() && e.getFlow() != null
					&& e.getFlow().getFlowType() == FlowType.PRODUCT_FLOW;
		}

		@Override
		protected ProcessDescriptor[] getItems(Exchange e) {
			if (e.getFlow() == null)
				return new ProcessDescriptor[0];
			FlowDao dao = new FlowDao(database);
			Set<Long> providerIds = dao.getProviders(e.getFlow().getId());
			Collection<ProcessDescriptor> list = cache.getAll(
					ProcessDescriptor.class, providerIds).values();
			ProcessDescriptor[] providers = list.toArray(
					new ProcessDescriptor[list.size()]);
			Arrays.sort(providers, (p1, p2) -> Strings.compare(
					Labels.getDisplayName(p1), Labels.getDisplayName(p2)));
			return providers;
		}

		@Override
		protected ProcessDescriptor getItem(Exchange e) {
			if (e.getDefaultProviderId() == 0)
				return null;
			return cache.get(ProcessDescriptor.class, e.getDefaultProviderId());
		}

		@Override
		protected String getText(ProcessDescriptor d) {
			if (d == null)
				return M.None;
			return Labels.getDisplayName(d);
		}

		@Override
		protected void setItem(Exchange e, ProcessDescriptor d) {
			if (d == null)
				e.setDefaultProviderId(0);
			else
				e.setDefaultProviderId(d.getId());
			editor.setDirty(true);
		}
	}

	private class AvoidedProductModifier extends CheckBoxCellModifier<Exchange> {

		@Override
		public boolean canModify(Exchange e) {
			Process process = editor.getModel();
			if (Objects.equals(process.getQuantitativeReference(), e))
				return false;
			if (e.getFlow() == null)
				return false;
			if (e.getFlow().getFlowType() != FlowType.PRODUCT_FLOW)
				return false;
			return true;
		}

		@Override
		protected boolean isChecked(Exchange e) {
			return e.isAvoidedProduct();
		}

		@Override
		protected void setChecked(Exchange e, boolean value) {
			if (e.isAvoidedProduct() == value)
				return;
			e.setAvoidedProduct(value);
			e.setInput(value);
			editor.setDirty(true);
		}
	}

	private class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parent, Object obj) {
			if (!(obj instanceof Exchange))
				return false;
			Exchange e = (Exchange) obj;
			if (e.isAvoidedProduct())
				return !forInputs;
			else
				return e.isInput() == forInputs;
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
