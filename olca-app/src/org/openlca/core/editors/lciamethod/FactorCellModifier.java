package org.openlca.core.editors.lciamethod;

import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Item;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.LCIAFactor;
import org.openlca.core.model.Unit;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FactorCellModifier implements ICellModifier {

	private String[] flowPropertyNames = new String[0];
	private String[] unitNames = new String[0];
	private Logger log = LoggerFactory.getLogger(getClass());
	private TableViewer viewer;
	private IDatabase database;

	public FactorCellModifier(TableViewer viewer, IDatabase database) {
		this.database = database;
		this.viewer = viewer;
	}

	private void updateCellEditors(final LCIAFactor factor) {
		Flow flow = factor.getFlow();
		UnitGroup unitGroup = null;

		// load flow information and unit group
		try {
			unitGroup = database
					.select(UnitGroup.class, factor.getFlowPropertyFactor()
							.getFlowProperty().getUnitGroupId());
		} catch (final Exception e) {
			log.error(
					"Loading flow information and unit group from database failed",
					e);
		}

		if (unitGroup != null) {
			// update cell editors
			flowPropertyNames = new String[flow.getFlowPropertyFactors().length];
			for (int i = 0; i < flow.getFlowPropertyFactors().length; i++) {
				flowPropertyNames[i] = flow.getFlowPropertyFactors()[i]
						.getFlowProperty().getName();
			}
			((ComboBoxCellEditor) viewer.getCellEditors()[2])
					.setItems(flowPropertyNames);

			unitNames = new String[unitGroup.getUnits().length];
			for (int i = 0; i < unitNames.length; i++) {
				unitNames[i] = unitGroup.getUnits()[i].getName();
			}
			((ComboBoxCellEditor) viewer.getCellEditors()[3])
					.setItems(unitNames);
		}

	}

	@Override
	public boolean canModify(final Object element, final String property) {
		boolean canModify = true;
		if (property.equals(FactorTable.FLOW)
				|| property.equals(FactorTable.CATEGORY)) {
			// all columns except flow and category column can be edited
			canModify = false;
		}
		return canModify;
	}

	@Override
	public Object getValue(final Object element, final String property) {
		Object v = null;
		if (element instanceof LCIAFactor) {
			final LCIAFactor factor = (LCIAFactor) element;
			// update the cell editors
			updateCellEditors(factor);
			if (property.equals(FactorTable.PROPERTY)) {
				// get flow property selection index
				int j = 0;
				while (v == null && j < flowPropertyNames.length) {
					if (factor.getFlowPropertyFactor().getFlowProperty()
							.getName().equals(flowPropertyNames[j])) {
						v = new Integer(j);
					} else {
						j++;
					}
				}
			} else if (property.equals(FactorTable.UNIT)) {
				// get unit selection index
				int j = 0;
				while (v == null && j < unitNames.length) {
					if (factor.getUnit().getName().equals(unitNames[j])) {
						v = new Integer(j);
					} else {
						j++;
					}
				}
			} else if (property.equals(FactorTable.VALUE)) {
				// get value
				v = Double.toString(factor.getValue());
			} else if (property.equals(FactorTable.UNCERTAINTY)) {
				v = factor;
			}
		}

		return v != null ? v : "";
	}

	@Override
	public void modify(Object element, final String property, final Object value) {
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		try {
			if (element instanceof LCIAFactor) {
				final LCIAFactor factor = (LCIAFactor) element;
				Flow flow = factor.getFlow();
				if (property.equals(FactorTable.PROPERTY)) {
					// set flow property
					boolean set = false;
					int i = 0;
					while (!set && i < flow.getFlowPropertyFactors().length) {
						final FlowPropertyFactor flowPropertyFactor = flow
								.getFlowPropertyFactors()[i];
						if (flowPropertyFactor
								.getFlowProperty()
								.getName()
								.equals(flowPropertyNames[Integer
										.parseInt(value.toString())])) {
							factor.setFlowPropertyFactor(flowPropertyFactor);
							factor.setUnit(database.select(
									UnitGroup.class,
									flowPropertyFactor.getFlowProperty()
											.getUnitGroupId())
									.getReferenceUnit());
							set = true;
						} else {
							i++;
						}
					}
				} else if (property.equals(FactorTable.UNIT)) {
					// set unit
					boolean set = false;
					int i = 0;
					final Unit[] units = database.select(
							UnitGroup.class,
							factor.getFlowPropertyFactor().getFlowProperty()
									.getUnitGroupId()).getUnits();
					while (!set && i < units.length) {
						if (units[i].getName().equals(
								unitNames[Integer.parseInt(value.toString())])) {
							factor.setUnit(units[i]);
							set = true;
						} else {
							i++;
						}
					}
				} else if (property.equals(FactorTable.VALUE)) {
					// set value
					factor.setValue(Double.parseDouble(value.toString()));
				}
			}
		} catch (final Exception e) {
			log.error("Modifying element failed", e);
		}
	}
}