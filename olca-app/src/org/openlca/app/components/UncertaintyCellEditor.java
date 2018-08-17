package org.openlca.app.components;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
import org.openlca.expressions.FormulaInterpreter;

/**
 * An uncertainty cell editor for exchanges, LCIA factors, parameters, and
 * parameter redefinitions.
 */
public class UncertaintyCellEditor extends DialogCellEditor {

	private ModelEditor<?> editor;
	private FormulaInterpreter interpreter;
	private long interpreterScope = -1;

	// types for which this cell editor can be used
	private ImpactFactor factor;
	private Exchange exchange;
	private Parameter parameter;
	private ParameterRedef parameterRedef;

	public UncertaintyCellEditor(Composite parent, ModelEditor<?> editor) {
		super(parent);
		this.editor = editor;
		if (editor instanceof ProcessEditor) {
			ProcessEditor e = (ProcessEditor) editor;
			// interpreter = e.getInterpreter(); TODO: Formulas.getInterpreter
			interpreterScope = e.getModel().getId();
		}
	}

	public UncertaintyCellEditor(Composite parent) {
		super(parent);
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
			u = parameter.getUncertainty();
		} else if (value instanceof ParameterRedef) {
			parameterRedef = (ParameterRedef) value;
			u = parameterRedef.getUncertainty();
		}
		super.doSetValue(u == null ? "none" : u.toString());
	}

	@Override
	protected Object openDialogBox(Control control) {
		Uncertainty initial = getInitial();
		UncertaintyDialog dialog = new UncertaintyDialog(control.getShell(),
				initial);
		if (interpreter != null)
			dialog.setInterpreter(interpreter, interpreterScope);
		if (dialog.open() != Window.OK)
			return null;
		Uncertainty uncertainty = dialog.getUncertainty();
		setUncertainty(uncertainty);
		return exchange != null ? exchange : factor;
	}

	private void setUncertainty(Uncertainty u) {
		if (u.distributionType == UncertaintyType.NONE)
			u = null;
		if (exchange != null)
			exchange.uncertainty = u;
		else if (factor != null)
			factor.uncertainty = u;
		else if (parameter != null)
			parameter.setUncertainty(u);
		else if (parameterRedef != null)
			parameterRedef.setUncertainty(u);
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
			uncertainty = parameter.getUncertainty();
			val = parameter.getValue();
		} else if (parameterRedef != null) {
			uncertainty = parameterRedef.getUncertainty();
			val = parameterRedef.getValue();
		}
		if (uncertainty != null)
			return uncertainty;
		else
			return Uncertainty.none(val);
	}

}
