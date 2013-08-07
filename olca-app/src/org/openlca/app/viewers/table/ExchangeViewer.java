package org.openlca.app.viewers.table;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.components.ObjectDialog;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.CategoryPath;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Expression;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Unit;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class ExchangeViewer extends AbstractTableViewer<Exchange> {

	private interface LABEL {
		String FLOW = Messages.Flow;
		String CATEGORY = Messages.Category;
		String FLOW_PROPERTY = Messages.FlowProperty;
		String UNIT = Messages.Unit;
		String AMOUNT = Messages.Amount;
	}

	public static final int INPUTS = 0x01;
	public static final int OUTPUTS = 0x02;
	public static final int PRODUCTS = 0x04;
	public static final int WASTES = 0x08;
	public static final int ELEMENTARIES = 0x10;
	public static final int ALL_TYPES = ELEMENTARIES | PRODUCTS | WASTES;

	public enum ViewMode {

		VALUE, FORMULA;

	}

	private final static String[] COLUMN_HEADERS = { LABEL.FLOW,
			LABEL.CATEGORY, LABEL.FLOW_PROPERTY, LABEL.UNIT, LABEL.AMOUNT };

	private FlowDao flowDao;
	private ViewMode viewMode;
	private Process process;
	private int direction;
	private int types;

	public ExchangeViewer(Composite parent, IDatabase database, int direction,
			int types) {
		super(parent);
		this.flowDao = new FlowDao(database);
		if (direction == 0 || types == 0)
			throw new IllegalArgumentException("Direction and type must be set");
		if (direction == (INPUTS | OUTPUTS))
			throw new IllegalArgumentException("Direction must be unamiguous");
		this.direction = direction;
		this.types = types;

		getCellModifySupport().support(LABEL.FLOW_PROPERTY,
				new FlowPropertyModifier());
		getCellModifySupport().support(LABEL.UNIT, new UnitModifier());
		getCellModifySupport().support(LABEL.AMOUNT, new AmountModifier());
	}

	private boolean matches(FlowType type) {
		switch (type) {
		case PRODUCT_FLOW:
			return is(PRODUCTS, types);
		case WASTE_FLOW:
			return is(WASTES, types);
		case ELEMENTARY_FLOW:
			return is(ELEMENTARIES, types);
		default:
			return false;
		}
	}

	private FlowType[] getFlowTypes() {
		List<FlowType> flowTypes = new ArrayList<>();
		if (is(PRODUCTS, types))
			flowTypes.add(FlowType.PRODUCT_FLOW);
		if (is(WASTES, types))
			flowTypes.add(FlowType.WASTE_FLOW);
		if (is(ELEMENTARIES, types))
			flowTypes.add(FlowType.ELEMENTARY_FLOW);
		return flowTypes.toArray(new FlowType[flowTypes.size()]);
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	private boolean is(int value, int flag) {
		return (value & flag) != 0;
	}

	public void setInput(Process process) {
		this.process = process;
		if (process == null)
			setInput(new Exchange[0]);
		else {
			if (is(direction, INPUTS))
				setInput(process.getInputs(getFlowTypes()));
			else if (is(direction, OUTPUTS))
				setInput(process.getOutputs(getFlowTypes()));
		}
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ExchangeLabelProvider();
	}

	@OnCreate
	protected void onCreate() {
		BaseDescriptor[] descriptors = ObjectDialog.multiSelect(ModelType.FLOW,
				new FlowTypeFilter());
		if (descriptors != null)
			for (BaseDescriptor descriptor : descriptors)
				add((FlowDescriptor) descriptor);
	}

	private void add(FlowDescriptor descriptor) {
		Exchange exchange = new Exchange();
		Flow flow = flowDao.getForId(descriptor.getId());
		exchange.setFlow(flow);
		exchange.setFlowPropertyFactor(flow.getReferenceFactor());
		exchange.setUnit(flow.getReferenceFactor().getFlowProperty()
				.getUnitGroup().getReferenceUnit());
		exchange.setInput(is(direction, INPUTS));
		fireModelChanged(ModelChangeType.CREATE, exchange);
		setInput(process);
	}

	@OnRemove
	protected void onRemove() {
		for (Exchange exchange : getAllSelected())
			fireModelChanged(ModelChangeType.REMOVE, exchange);
		setInput(process);
	}

	@OnDrop
	protected void onDrop(FlowDescriptor descriptor) {
		if (descriptor != null && matches(descriptor.getFlowType()))
			add(descriptor);
	}

	private class ExchangeLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;

			if (columnIndex == 0)
				switch (exchange.getFlow().getFlowType()) {
				case ELEMENTARY_FLOW:
					return ImageType.FLOW_SUBSTANCE.get();
				case PRODUCT_FLOW:
					return ImageType.FLOW_PRODUCT.get();
				case WASTE_FLOW:
					return ImageType.FLOW_WASTE.get();
				}
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Exchange))
				return null;
			Exchange exchange = (Exchange) element;
			switch (columnIndex) {
			case 0:
				return exchange.getFlow().getName();
			case 1:
				return CategoryPath.getFull(exchange.getFlow().getCategory());
			case 2:
				return exchange.getFlowPropertyFactor().getFlowProperty()
						.getName();
			case 3:
				return exchange.getUnit().getName();
			case 4:
				return getAmountText(exchange);
			case 5:
				if (exchange.getDefaultProviderId() != 0)
					return Long.toString(exchange.getDefaultProviderId());
				else
					return "-";
			}
			return null;
		}
	}

	private class FlowTypeFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			if (element instanceof CategoryElement)
				return containsMatchingElement((CategoryElement) element);
			if (!(element instanceof ModelElement))
				return false;
			return matches((ModelElement) element);
		}

		private boolean containsMatchingElement(CategoryElement element) {
			for (INavigationElement<?> child : element.getChildren())
				if (child instanceof CategoryElement) {
					if (containsMatchingElement((CategoryElement) child))
						return true;
				} else if (matches((ModelElement) child))
					return true;
			return false;
		}

		private boolean matches(ModelElement element) {
			BaseDescriptor descriptor = element.getContent();
			if (descriptor.getModelType() != ModelType.FLOW)
				return false;
			FlowDescriptor flowDescriptor = (FlowDescriptor) descriptor;
			return ExchangeViewer.this.matches(flowDescriptor.getFlowType());
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
				fireModelChanged(ModelChangeType.CHANGE, element);
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
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

	private class AmountModifier extends TextCellModifier<Exchange> {

		@Override
		protected String getText(Exchange element) {
			return getAmountText(element);
		}

		@Override
		protected void setText(Exchange element, String text) {
			Expression expression = element.getResultingAmount();
			try {
				// is number
				double value = Double.parseDouble(text);
				if (expression.getValue() != value
						|| expression.getFormula() != null) {
					expression.setFormula(null);
					expression.setValue(value);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {
				// is formula
				if (!Objects.equals(text, expression.getFormula())) {
					expression.setFormula(text);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			}
		}

	}

	private String getAmountText(Exchange exchange) {
		if (viewMode == ViewMode.VALUE
				|| StringUtils.isEmpty(exchange.getResultingAmount()
						.getFormula()))
			return Double.toString(exchange.getResultingAmount().getValue());
		else
			return exchange.getResultingAmount().getFormula();
	}

}
