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
import org.openlca.app.util.UI;
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

	public enum Direction {

		INPUT, OUTPUT;

	}

	public enum Type {

		PRODUCT(FlowType.PRODUCT_FLOW),

		WASTE(FlowType.WASTE_FLOW),

		ELEMENTARY(FlowType.ELEMENTARY_FLOW),

		AVOIDED_PRODUCT(FlowType.PRODUCT_FLOW);

		private FlowType type;

		private Type(FlowType type) {
			this.type = type;
		}

	}

	public enum ViewMode {

		VALUE, FORMULA;

	}

	private final static String[] COLUMN_HEADERS = { LABEL.FLOW,
			LABEL.CATEGORY, LABEL.FLOW_PROPERTY, LABEL.UNIT, LABEL.AMOUNT };

	private FlowDao flowDao;
	private Direction direction;
	private Type type;
	private ViewMode viewMode;
	private Process process;

	public ExchangeViewer(Composite parent, IDatabase database,
			Direction direction, Type type) {
		super(parent);
		this.flowDao = new FlowDao(database);
		if (direction == null || type == null)
			throw new IllegalArgumentException("Direction and type must be set");
		if (direction == Direction.OUTPUT && type == Type.AVOIDED_PRODUCT)
			throw new IllegalArgumentException(
					"Avoided products can only be inputs");
		this.direction = direction;
		this.type = type;

		getCellModifySupport().support(LABEL.FLOW_PROPERTY,
				new FlowPropertyModifier());
		getCellModifySupport().support(LABEL.UNIT, new UnitModifier());
		getCellModifySupport().support(LABEL.AMOUNT, new AmountModifier());
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	public void setInput(Process process) {
		this.process = process;
		if (process == null)
			setInput(new Exchange[0]);
		else {
			if (direction == Direction.INPUT)
				if (type != Type.AVOIDED_PRODUCT)
					setInput(process.getInputs(type.type));
				else {
					List<Exchange> exchanges = new ArrayList<>();
					for (Exchange exchange : process.getInputs(type.type))
						if (exchange.isAvoidedProduct())
							exchanges.add(exchange);
					setInput(exchanges.toArray(new Exchange[exchanges.size()]));
				}
			else if (direction == Direction.OUTPUT)
				setInput(process.getOutputs(type.type));
		}
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ExchangeLabelProvider();
	}

	@OnCreate
	protected void onCreate() {
		ObjectDialog dialog = new ObjectDialog(UI.shell(), ModelType.FLOW,
				false);
		dialog.addFilter(new FlowTypeFilter());
		if (dialog.open() == ObjectDialog.OK)
			add((FlowDescriptor) dialog.getSelection());
	}

	private void add(FlowDescriptor descriptor) {
		Exchange exchange = new Exchange();
		Flow flow = flowDao.getForId(descriptor.getId());
		exchange.setFlow(flow);
		exchange.setFlowPropertyFactor(flow.getReferenceFactor());
		exchange.setUnit(flow.getReferenceFactor().getFlowProperty()
				.getUnitGroup().getReferenceUnit());
		exchange.setInput(direction == Direction.INPUT);
		exchange.setAvoidedProduct(type == Type.AVOIDED_PRODUCT);
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
		if (descriptor != null && descriptor.getFlowType() == type.type)
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
			return flowDescriptor.getFlowType() == type.type;
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
