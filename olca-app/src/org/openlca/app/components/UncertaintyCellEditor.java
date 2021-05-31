package org.openlca.app.components;

import javax.annotation.Nullable;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.editors.ModelEditor;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;

/**
 * An uncertainty cell editor for exchanges, LCIA factors, parameters, and
 * parameter redefinitions.
 */
public class UncertaintyCellEditor extends DialogCellEditor {

	private final ModelEditor<?> editor;

	// types for which this cell editor can be used
	private ImpactFactor factor;
	private Exchange exchange;
	private Parameter parameter;
	private ParameterRedef parameterRedef;

	public UncertaintyCellEditor(Composite parent, ModelEditor<?> editor) {
		super(parent);
		this.editor = editor;
	}

	@Override
	protected void doSetValue(Object value) {
		Uncertainty u = null;
		if (value instanceof ImpactFactor) {
			factor = (ImpactFactor) value;
			u = factor.uncertainty;
		} else if (value instanceof Exchange) {
			exchange = (Exchange) value;
			u = exchange.uncertainty;
		} else if (value instanceof Parameter) {
			parameter = (Parameter) value;
			u = parameter.uncertainty;
		} else if (value instanceof ParameterRedef) {
			parameterRedef = (ParameterRedef) value;
			u = parameterRedef.uncertainty;
		}
		super.doSetValue(u == null ? "none" : u.toString());
	}

	@Override
	protected Object openDialogBox(Control control) {
		var option = UncertaintyDialog.open(getInitial());
		if (option.isPresent()) {
			var u = option.get();
			if (u.distributionType == null
					|| u.distributionType == UncertaintyType.NONE) {
				setUncertainty(null);
			} else {
				setUncertainty(u);
			}
		}
		return exchange != null ? exchange : factor;
	}

	private void setUncertainty(@Nullable Uncertainty u) {
		if (exchange != null)
			exchange.uncertainty = u;
		else if (factor != null)
			factor.uncertainty = u;
		else if (parameter != null)
			parameter.uncertainty = u;
		else if (parameterRedef != null)
			parameterRedef.uncertainty = u;
		updateContents(Uncertainty.string(u));
		if (editor != null)
			editor.setDirty(true);
	}

	private Uncertainty getInitial() {
		double val = 1;
		Uncertainty uncertainty = null;
		if (exchange != null) {
			uncertainty = exchange.uncertainty;
			val = exchange.amount;
		} else if (factor != null) {
			uncertainty = factor.uncertainty;
			val = factor.value;
		} else if (parameter != null) {
			uncertainty = parameter.uncertainty;
			val = parameter.value;
		} else if (parameterRedef != null) {
			uncertainty = parameterRedef.uncertainty;
			val = parameterRedef.value;
		}
		if (uncertainty != null)
			return uncertainty;
		else
			return Uncertainty.none(val);
	}

}
