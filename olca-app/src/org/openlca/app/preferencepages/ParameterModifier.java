package org.openlca.app.preferencepages;

import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Item;
import org.openlca.app.Messages;
import org.openlca.app.util.Dialog;
import org.openlca.core.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The cell modifier of the parameter table. */
class ParameterModifier implements ICellModifier {

	private final String NAME = Messages.Name;
	private final String DESCRIPTION = Messages.Description;
	private final String NUMERIC_VALUE = Messages.Amount;

	private Logger log = LoggerFactory.getLogger(getClass());
	private TableViewer viewer;

	public ParameterModifier(TableViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public boolean canModify(Object element, String property) {
		return true;
	}

	@Override
	public Object getValue(Object element, String property) {
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		if (!(element instanceof Parameter) || property == null)
			return null;
		Parameter parameter = (Parameter) element;
		if (property.equals(NAME))
			return parameter.getName();
		else if (property.equals(NUMERIC_VALUE))
			return parameter.getValue();
		else if (property.equals(DESCRIPTION))
			return parameter.getDescription();
		else
			return null;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		if (element instanceof Item) {
			element = ((Item) element).getData();
		}
		if (!(element instanceof Parameter))
			return;
		Parameter parameter = (Parameter) element;
		log.trace("modify parameter {}", parameter);
		log.trace("modify property {} with value {}", property, value);
		setValue(property, (String) value, parameter);
		viewer.refresh();
	}

	private void setValue(String property, String value, Parameter parameter) {
		if (property.equals(NAME)) {
			setParameterName(parameter, value);
		} else if (property.equals(NUMERIC_VALUE)) {
			setParameterValue(parameter, value);

		} else if (property.equals(DESCRIPTION)) {
			log.trace("set description to {}", value);
			parameter.setDescription(value);
		}
	}

	private void setParameterName(Parameter parameter, String value) {
		log.trace("set name to {}", value);
		if (Parameter.isValidName(value)) {
			parameter.setName(value);
		} else {
			Dialog.showError(viewer.getControl().getShell(),
					"Invalid parameter name: " + value);
		}
	}

	private void setParameterValue(Parameter parameter, String value) {
		log.trace("set value to {}", value);
		try {
			double val = Double.parseDouble(value);
			parameter.setValue(val);
		} catch (Exception e) {
			Dialog.showError(viewer.getControl().getShell(), value
					+ " is not a valid number.");
		}
	}

}
