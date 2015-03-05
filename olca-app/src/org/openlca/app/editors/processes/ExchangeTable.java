package org.openlca.app.editors.processes;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.ModelSelectionDialog;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.Labels;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.Viewers;
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
import org.openlca.io.CategoryPath;

/**
 * The table for the display and editing of inputs or outputs of process
 * exchanges. Avoided products are inputs that are shown on the output site in
 * this table.
 * 
 */
class ExchangeTable {

	private final boolean forInputs;
	private boolean showFormulas = true;
	private final ProcessEditor editor;
	private IDatabase database = Database.get();
	private EntityCache cache = Cache.getEntityCache();

	private final String FLOW = Messages.Flow;
	private final String CATEGORY = Messages.Category;
	private final String FLOW_PROPERTY = Messages.FlowProperty;
	private final String UNIT = Messages.Unit;
	private final String AMOUNT = Messages.Amount;
	private final String PEDIGREE = Messages.PedigreeUncertainty;
	private final String DEFAULT_PROVIDER = Messages.DefaultProvider;
	private final String UNCERTAINTY = Messages.Uncertainty;
	private final String AVOIDED_PRODUCT = Messages.AvoidedProduct;

	private TableViewer viewer;

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
		this.editor = editor;
		editor.getParameterSupport().afterEvaluation(() -> viewer.refresh());
	}

	private void render(Section section, FormToolkit toolkit) {
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		viewer = Tables.createViewer(composite, getColumns());
		ExchangeLabelProvider labelProvider = new ExchangeLabelProvider();
		viewer.setLabelProvider(labelProvider);
		bindModifiers();
		Tables.addDropSupport(viewer, (droppedModels) -> add(droppedModels));
		viewer.addFilter(new Filter());
		bindActions(section, viewer);
		bindDoubleClick(viewer);
		Tables.bindColumnWidths(viewer, 0.2, 0.15, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1);
		ExchangeTableSorters.register(viewer, labelProvider);
	}

	void setInput(Process process) {
		viewer.setInput(process.getExchanges());
	}

	private void bindModifiers() {
		ModifySupport<Exchange> modifySupport = new ModifySupport<>(viewer);
		modifySupport.bind(FLOW_PROPERTY, new FlowPropertyModifier());
		modifySupport.bind(UNIT, new UnitModifier());
		modifySupport.bind(AMOUNT, new AmountModifier());
		modifySupport.bind(PEDIGREE, new PedigreeCellEditor(viewer, editor));
		modifySupport.bind(UNCERTAINTY,
				new UncertaintyCellEditor(viewer.getTable(), editor));
		if (forInputs)
			modifySupport.bind(DEFAULT_PROVIDER, new ProviderModifier());
		if (!forInputs)
			modifySupport.bind(AVOIDED_PRODUCT, new AvoidedProductModifier());
	}

	private void bindActions(Section section, final TableViewer viewer) {
		Action add = Actions.onAdd(() -> onAdd());
		Action remove = Actions.onRemove(() -> onRemove());
		Action formulaSwitch = new FormulaSwitchAction();
		Action clipboard = TableClipboard.onCopy(viewer);
		Actions.bind(section, add, remove, formulaSwitch);
		Actions.bind(viewer, add, remove, clipboard);
		Tables.onDeletePressed(viewer, (e) -> onRemove());
	}

	private void bindDoubleClick(final TableViewer viewer) {
		Tables.onDoubleClick(viewer, (e) -> {
			TableItem item = Tables.getItem(viewer, e);
			if (item == null) {
				onAdd();
				return;
			}
			Exchange exchange = Viewers.getFirstSelected(viewer);
			if (exchange != null && exchange.getFlow() != null)
				App.openEditor(exchange.getFlow());
		});
	}

	private String[] getColumns() {
		if (forInputs)
			return new String[] { FLOW, CATEGORY, FLOW_PROPERTY, UNIT, AMOUNT,
					UNCERTAINTY, DEFAULT_PROVIDER, PEDIGREE };
		else
			return new String[] { FLOW, CATEGORY, FLOW_PROPERTY, UNIT, AMOUNT,
					UNCERTAINTY, AVOIDED_PRODUCT, PEDIGREE };
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
		selection.forEach((e) -> process.getExchanges().remove(e));
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
			Exchange exchange = new Exchange();
			FlowDao flowDao = new FlowDao(Database.get());
			Flow flow = flowDao.getForId(descriptor.getId());
			exchange.setFlow(flow);
			exchange.setFlowPropertyFactor(flow.getReferenceFactor());
			exchange.setUnit(flow.getReferenceFactor().getFlowProperty()
					.getUnitGroup().getReferenceUnit());
			exchange.setAmountValue(1.0);
			exchange.setInput(forInputs);
			process.getExchanges().add(exchange);
		}
		viewer.setInput(process.getExchanges());
		editor.setDirty(true);
		editor.postEvent(editor.EXCHANGES_CHANGED, this);
	}

	private class ExchangeLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int col) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			if (col == 0)
				return getFlowTypeIcon(exchange);
			if (!forInputs && col == 6)
				return getAvoidedCheck(exchange);
			return null;
		}

		private Image getAvoidedCheck(Exchange exchange) {
			if (exchange.getFlow() == null)
				return null;
			if (exchange.getFlow().getFlowType() != FlowType.PRODUCT_FLOW)
				return null;
			Process process = editor.getModel();
			if (Objects.equals(process.getQuantitativeReference(), exchange))
				return null;
			else
				return exchange.isAvoidedProduct() ? ImageType.CHECK_TRUE.get()
						: ImageType.CHECK_FALSE.get();
		}

		private Image getFlowTypeIcon(Exchange exchange) {
			if (exchange == null)
				return null;
			Flow flow = exchange.getFlow();
			if (flow == null || flow.getFlowType() == null)
				return null;
			switch (flow.getFlowType()) {
			case ELEMENTARY_FLOW:
				return ImageType.FLOW_SUBSTANCE.get();
			case PRODUCT_FLOW:
				return ImageType.FLOW_PRODUCT.get();
			case WASTE_FLOW:
				return ImageType.FLOW_WASTE.get();
			default:
				return null;
			}
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			switch (columnIndex) {
			case 0:
				return Labels.getDisplayName(exchange.getFlow());
			case 1:
				return CategoryPath.getShort(exchange.getFlow().getCategory());
			case 2:
				return Labels.getDisplayName(exchange.getFlowPropertyFactor()
						.getFlowProperty());
			case 3:
				return Labels.getDisplayName(exchange.getUnit());
			case 4:
				return getAmountText(exchange);
			case 5:
				return UncertaintyLabel.get(exchange.getUncertainty());
			case 6:
				if (forInputs)
					return getDefaultProvider(exchange);
				else
					return null;
			case 7:
				return exchange.getPedigreeUncertainty();
			}
			return null;
		}

		private String getDefaultProvider(Exchange exchange) {
			if (exchange.getDefaultProviderId() == 0)
				return null;
			ProcessDescriptor descriptor = cache.get(ProcessDescriptor.class,
					exchange.getDefaultProviderId());
			if (descriptor == null)
				return null;
			return Labels.getDisplayName(descriptor);
		}

		private String getAmountText(Exchange exchange) {
			if (!showFormulas || exchange.getAmountFormula() == null)
				return Double.toString(exchange.getAmountValue());
			else
				return exchange.getAmountFormula();
		}
	}

	private class FlowPropertyModifier extends
			ComboBoxCellModifier<Exchange, FlowPropertyFactor> {

		@Override
		protected FlowPropertyFactor[] getItems(Exchange element) {
			List<FlowPropertyFactor> factors = element.getFlow()
					.getFlowPropertyFactors();
			return factors.toArray(new FlowPropertyFactor[factors.size()]);
		}

		@Override
		protected FlowPropertyFactor getItem(Exchange element) {
			return element.getFlowPropertyFactor();
		}

		@Override
		protected String getText(FlowPropertyFactor value) {
			return value.getFlowProperty().getName();
		}

		@Override
		protected void setItem(Exchange element, FlowPropertyFactor item) {
			if (!Objects.equals(element.getFlowPropertyFactor(), item)) {
				element.setFlowPropertyFactor(item);
				editor.setDirty(true);
			}
		}

	}

	private class UnitModifier extends ComboBoxCellModifier<Exchange, Unit> {

		@Override
		protected Unit[] getItems(Exchange element) {
			List<Unit> units = element.getFlowPropertyFactor()
					.getFlowProperty().getUnitGroup().getUnits();
			return units.toArray(new Unit[units.size()]);
		}

		@Override
		protected Unit getItem(Exchange element) {
			return element.getUnit();
		}

		@Override
		protected String getText(Unit value) {
			return value.getName();
		}

		@Override
		protected void setItem(Exchange element, Unit item) {
			if (!Objects.equals(element.getUnit(), item)) {
				element.setUnit(item);
				editor.setDirty(true);
			}
		}
	}

	private class AmountModifier extends TextCellModifier<Exchange> {

		@Override
		protected String getText(Exchange element) {
			if (element.getAmountFormula() == null)
				return Double.toString(element.getAmountValue());
			return element.getAmountFormula();
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
					Error.showBox(Messages.InvalidFormula, text + " "
							+ Messages.IsInvalidFormula);
				}
			}
		}
	}

	private class ProviderModifier extends
			ComboBoxCellModifier<Exchange, ProcessDescriptor> {

		@Override
		public boolean canModify(Exchange element) {
			return element.isInput() && element.getFlow() != null
					&& element.getFlow().getFlowType() == FlowType.PRODUCT_FLOW;
		}

		@Override
		protected ProcessDescriptor[] getItems(Exchange element) {
			if (element.getFlow() == null)
				return new ProcessDescriptor[0];
			FlowDao dao = new FlowDao(database);
			Set<Long> providerIds = dao.getProviders(element.getFlow().getId());
			Collection<ProcessDescriptor> descriptors = cache.getAll(
					ProcessDescriptor.class, providerIds).values();
			ProcessDescriptor[] array = new ProcessDescriptor[descriptors
					.size() + 1];
			int i = 1;
			for (ProcessDescriptor d : descriptors) {
				array[i] = d;
				i++;
			}
			return array;
		}

		@Override
		protected ProcessDescriptor getItem(Exchange element) {
			if (element.getDefaultProviderId() == 0)
				return null;
			return cache.get(ProcessDescriptor.class,
					element.getDefaultProviderId());
		}

		@Override
		protected String getText(ProcessDescriptor value) {
			if (value == null)
				return Messages.None;
			return Labels.getDisplayName(value);
		}

		@Override
		protected void setItem(Exchange element, ProcessDescriptor item) {
			if (item == null)
				element.setDefaultProviderId(0);
			else
				element.setDefaultProviderId(item.getId());
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
		protected boolean isChecked(Exchange element) {
			return element.isAvoidedProduct();
		}

		@Override
		protected void setChecked(Exchange element, boolean value) {
			if (element.isAvoidedProduct() == value)
				return;
			element.setAvoidedProduct(value);
			element.setInput(value);
			editor.setDirty(true);
		}
	}

	private class Filter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (!(element instanceof Exchange))
				return false;
			Exchange exchange = (Exchange) element;
			if (exchange.isAvoidedProduct())
				return !forInputs;
			else
				return exchange.isInput() == forInputs;
		}
	}

	private class FormulaSwitchAction extends Action {

		public FormulaSwitchAction() {
			setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
			setText(Messages.ShowValues);
		}

		@Override
		public void run() {
			showFormulas = !showFormulas;
			if (showFormulas) {
				setImageDescriptor(ImageType.NUMBER_ICON.getDescriptor());
				setText(Messages.ShowValues);
			} else {
				setImageDescriptor(ImageType.FORMULA_ICON.getDescriptor());
				setText(Messages.ShowFormulas);
			}
			viewer.refresh();
		}
	}

}
