package org.openlca.core.editors.process;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Item;
import org.openlca.core.database.FlowDao;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.UncertaintyDistributionType;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.ui.Labels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The cell modifier for the input output tables. */
class ExchangeCellModifier implements ICellModifier {

	private Logger log = LoggerFactory.getLogger(getClass());
	private TableViewer viewer;
	private IDatabase database;
	private String distributionValues[];
	private boolean forInputs;

	public ExchangeCellModifier(TableViewer viewer, IDatabase database,
			boolean forInputs) {
		this.viewer = viewer;
		this.database = database;
		this.forInputs = forInputs;
		intDistributionValues();
	}

	private void intDistributionValues() {
		UncertaintyDistributionType[] types = UncertaintyDistributionType
				.values();
		distributionValues = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			distributionValues[i] = Labels.uncertaintyType(types[i]);
		}
		ComboBoxCellEditor editor = getComboEditor(ExchangeTable.UNCERTAINTY_COLUMN);
		if (editor != null)
			editor.setItems(distributionValues);
	}

	@Override
	public boolean canModify(Object element, String property) {
		if (!(element instanceof Exchange) || property == null)
			return false;
		Exchange exchange = (Exchange) element;
		if (property.equals(ExchangeTable.CATEGORY)
				|| property.equals(ExchangeTable.FLOW))
			return false;
		if (property.equals(ExchangeTable.AMOUNT))
			return exchange.getDistributionType() == null
					|| exchange.getDistributionType() == UncertaintyDistributionType.NONE;
		if (property.equals(ExchangeTable.AVOIDED_PRODUCT))
			return exchange.getFlow().getFlowType() != FlowType.ELEMENTARY_FLOW
					&& (exchange.isInput() == exchange.isAvoidedProduct());
		if (property.equals(ExchangeTable.PROVIDER))
			return exchange.isInput()
					&& exchange.getFlow().getFlowType() != FlowType.ELEMENTARY_FLOW;
		return true;
	}

	@Override
	public Object getValue(Object element, String property) {
		if (!(element instanceof Exchange))
			return null;
		Exchange exchange = (Exchange) element;
		refreshComboValues(exchange);
		if (property.equals(ExchangeTable.PROPERTY))
			return getPropertyIndex(exchange);
		if (property.equals(ExchangeTable.UNIT))
			return getUnitIndex(exchange);
		if (property.equals(ExchangeTable.AMOUNT))
			return exchange.getResultingAmount().getFormula();
		if (property.equals(ExchangeTable.UNCERTAINTY))
			return getDistributionIndex(exchange);
		if (property.equals(ExchangeTable.AVOIDED_PRODUCT))
			return exchange.isAvoidedProduct();
		if (property.equals(ExchangeTable.PEDIGREE))
			return exchange;
		if (property.equals(ExchangeTable.PROVIDER))
			return getProviderIndex(exchange);
		return null;
	}

	private int getProviderIndex(Exchange exchange) {
		Long providerId = exchange.getDefaultProviderId();
		if (providerId == null)
			return -1;
		ProcessDao dao = new ProcessDao(database.getEntityFactory());
		try {
			BaseDescriptor d = dao.getDescriptor(providerId);
			if (d == null || d.getName() == null)
				return -1;
			ComboBoxCellEditor editor = getComboEditor(ExchangeTable.PROVIDER_COLUMN);
			return getIndex(d.getName(), editor.getItems());
		} catch (Exception e) {
			log.error("Could not get default provider", e);
			return -1;
		}
	}

	/** Get the index of the selected uncertainty distribution type. */
	private int getDistributionIndex(Exchange exchange) {
		String val = Labels.uncertaintyType(exchange.getDistributionType());
		return getIndex(val, distributionValues);
	}

	private void refreshComboValues(Exchange exchange) {
		String[] flowPropertyNames = getFlowPropertyNames(exchange);
		ComboBoxCellEditor propEditor = getComboEditor(ExchangeTable.PROPERTY_COLUMN);
		propEditor.setItems(flowPropertyNames);
		String[] unitNames = getUnitNames(exchange);
		ComboBoxCellEditor unitEditor = getComboEditor(ExchangeTable.UNIT_COLUMN);
		unitEditor.setItems(unitNames);
		if (forInputs) {
			String[] providerNames = getProviderNames(exchange);
			ComboBoxCellEditor providerEditor = getComboEditor(ExchangeTable.PROVIDER_COLUMN);
			providerEditor.setItems(providerNames);
		}
	}

	private String[] getProviderNames(Exchange exchange) {
		Flow flow = exchange.getFlow();
		if (flow == null || flow.getFlowType() == FlowType.ELEMENTARY_FLOW)
			return new String[0];
		try {
			FlowDao dao = new FlowDao(database.getEntityFactory());
			List<BaseDescriptor> providers = dao.getProviders(flow);
			String[] names = new String[providers.size() + 1];
			names[0] = "";
			for (int i = 0; i < providers.size(); i++)
				names[i + 1] = providers.get(i).getDisplayName();
			Arrays.sort(names);
			return names;
		} catch (Exception e) {
			log.error("Failed to get providers", e);
			return new String[0];
		}
	}

	private int getUnitIndex(Exchange exchange) {
		if (exchange == null || exchange.getUnit() == null)
			return 0;
		String unitName = exchange.getUnit().getName();
		ComboBoxCellEditor unitEditor = getComboEditor(ExchangeTable.UNIT_COLUMN);
		String[] units = unitEditor.getItems();
		return getIndex(unitName, units);
	}

	private ComboBoxCellEditor getComboEditor(int column) {
		return ((ComboBoxCellEditor) viewer.getCellEditors()[column]);
	}

	private int getPropertyIndex(Exchange exchange) {
		if (exchange == null || exchange.getFlowPropertyFactor() == null)
			return 0;
		FlowProperty p = exchange.getFlowPropertyFactor().getFlowProperty();
		if (p == null)
			return 0;
		String name = p.getName();
		ComboBoxCellEditor propEditor = getComboEditor(ExchangeTable.PROPERTY_COLUMN);
		return getIndex(name, propEditor.getItems());
	}

	private int getIndex(String val, String[] values) {
		if (val == null || values == null)
			return -1;
		for (int i = 0; i < values.length; i++) {
			if (val.equals(values[i]))
				return i;
		}
		return -1;
	}

	private String[] getUnitNames(Exchange exchange) {
		UnitGroup unitGroup = getUnitGroup(exchange);
		if (unitGroup == null)
			return new String[0];
		List<Unit> units = unitGroup.getUnits();
		String[] vals = new String[units.size()];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = units.get(i).getName();
		}
		return vals;
	}

	private String[] getFlowPropertyNames(Exchange exchange) {
		Flow flow = exchange.getFlow();
		if (flow == null)
			return new String[0];
		List<FlowPropertyFactor> factors = flow.getFlowPropertyFactors();
		String[] vals = new String[factors.size()];
		for (int i = 0; i < vals.length; i++) {
			vals[i] = factors.get(i).getFlowProperty().getName();
		}
		return vals;
	}

	private UnitGroup getUnitGroup(Exchange exchange) {
		if (exchange == null || exchange.getFlowPropertyFactor() == null)
			return null;
		FlowPropertyFactor factor = exchange.getFlowPropertyFactor();
		try {
			return factor.getFlowProperty().getUnitGroup();
		} catch (Exception e) {
			log.error("Failed to load unit group " + exchange, e);
			return null;
		}
	}

	@Override
	public void modify(Object element, String property, Object value) {
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		if (!(element instanceof Exchange) || property == null)
			return;
		Exchange exchange = (Exchange) element;
		if (property.equals(ExchangeTable.PROPERTY))
			setProperty(value, exchange);
		else if (property.equals(ExchangeTable.UNIT))
			setUnit(value, exchange);
		else if (property.equals(ExchangeTable.AMOUNT))
			setAmount(value, exchange);
		else if (property.equals(ExchangeTable.UNCERTAINTY))
			setUncertainty(value, exchange);
		else if (property.equals(ExchangeTable.AVOIDED_PRODUCT))
			setAvoided(value, exchange);
		else if (property.equals(ExchangeTable.PROVIDER))
			setProvider(value, exchange);
		viewer.refresh();
	}

	private void setAvoided(Object value, Exchange exchange) {
		boolean avoided = Boolean.parseBoolean(value.toString());
		exchange.setAvoidedProduct(avoided);
		exchange.setInput(avoided);
	}

	private void setAmount(Object value, Exchange exchange) {
		String formula = value.toString().trim();
		if (formula.isEmpty())
			formula = "0";
		exchange.getResultingAmount().setFormula(formula);
	}

	private void setUncertainty(Object value, Exchange exchange) {
		int index = Integer.parseInt(value.toString());
		if (index == -1)
			exchange.setDistributionType(UncertaintyDistributionType.NONE);
		else {
			UncertaintyDistributionType[] vals = UncertaintyDistributionType
					.values();
			if (index < 0 || index >= vals.length)
				exchange.setDistributionType(UncertaintyDistributionType.NONE);
			else
				exchange.setDistributionType(vals[index]);
		}
	}

	private void setProperty(Object value, Exchange exchange) {
		String propName = getStringValue(value, ExchangeTable.PROPERTY_COLUMN);
		if (propName == null
				|| exchange.getFlowPropertyFactor().getFlowProperty().getName()
						.equals(propName))
			return;
		Flow flowInfo = exchange.getFlow();
		for (FlowPropertyFactor factor : flowInfo.getFlowPropertyFactors()) {
			FlowProperty prop = factor.getFlowProperty();
			if (!prop.getName().equals(propName))
				continue;
			exchange.setFlowPropertyFactor(factor);
			UnitGroup group = getUnitGroup(exchange);
			exchange.setUnit(group.getReferenceUnit());
			break;
		}
	}

	private void setUnit(Object value, Exchange exchange) {
		String unitName = getStringValue(value, ExchangeTable.UNIT_COLUMN);
		if (unitName == null)
			return;
		UnitGroup group = getUnitGroup(exchange);
		for (Unit unit : group.getUnits()) {
			if (!unit.getName().equals(unitName))
				continue;
			exchange.setUnit(unit);
			break;
		}
	}

	private void setProvider(Object value, Exchange exchange) {
		String providerName = getStringValue(value,
				ExchangeTable.PROVIDER_COLUMN);
		if (providerName == null || providerName.isEmpty()) {
			exchange.setDefaultProviderId(0);
			return;
		}
		FlowDao dao = new FlowDao(database.getEntityFactory());
		try {
			List<BaseDescriptor> descriptors = dao.getProviders(exchange
					.getFlow());
			for (BaseDescriptor d : descriptors) {
				if (providerName.equals(d.getDisplayName())) {
					exchange.setDefaultProviderId(d.getId());
					break;
				}
			}
		} catch (Exception e) {
			log.error("Failed to assign default provider");
		}
	}

	private String getStringValue(Object selection, int column) {
		ComboBoxCellEditor editor = getComboEditor(column);
		int index = Integer.parseInt(selection.toString());
		String[] vals = editor.getItems();
		if (index < 0 || index >= vals.length)
			return null;
		return vals[index];
	}

}