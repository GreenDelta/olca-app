package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Error;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Uncertainty;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.expressions.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The input of a parameter section.
 */
public class ParameterPageSupport {

	private Logger log = LoggerFactory.getLogger(getClass());

	private final List<ParameterPageListener> listeners = new ArrayList<>();

	private final FormulaInterpreter interpreter;
	private final ModelEditor<?> editor;
	private final Supplier<List<Parameter>> parameters;
	private final ParameterScope scope;

	public ParameterPageSupport(ModelEditor<?> editor,
			Supplier<List<Parameter>> parameters, ParameterScope scope) {
		this.editor = editor;
		this.parameters = parameters;
		this.scope = scope;
		this.interpreter = new FormulaInterpreter();
		refreshInterpreter();
	}

	public ModelEditor<?> getEditor() {
		return editor;
	}

	public List<Parameter> getParameters() {
		return parameters.get();
	}

	public FormulaInterpreter getInterpreter() {
		return interpreter;
	}

	public ParameterScope getScope() {
		return scope;
	}

	public void addListener(ParameterPageListener listener) {
		listeners.add(listener);
	}

	public void fireParameterChange() {
		log.trace("registered event: parameter changed");
		editor.setDirty(true);
		refreshInterpreter();
		try {
			log.trace("evaluate all parameter formulas");
			for (Parameter parameter : parameters.get()) {
				if (parameter.isInputParameter())
					continue;
				double val = eval(parameter.getFormula());
				parameter.setValue(val);
			}
			log.trace("inform parameter listeners");
			for (ParameterPageListener listener : listeners)
				listener.parameterChanged();
		} catch (Exception e) {
			Error.showBox(Messages.FormulaEvaluationFailed, e.getMessage());
		}
	}

	private void refreshInterpreter() {
		CategorizedEntity model = editor.getModel();
		log.trace("(re-)init formula interpreter for {}", model);
		IDatabase database = Database.get();
		ParameterDao dao = new ParameterDao(database);
		interpreter.clear();
		for (Parameter globalParam : dao.getGlobalParameters()) {
			interpreter.getGlobalScope().bind(globalParam.getName(),
					Double.toString(globalParam.getValue()));
		}
		Scope scope = interpreter.createScope(model.getId());
		for (Parameter param : parameters.get()) {
			if (param.isInputParameter())
				scope.bind(param.getName(), Double.toString(param.getValue()));
			else
				scope.bind(param.getName(), param.getFormula());
		}
	}

	public void eval(Exchange e) {
		try {
			if (e.getAmountFormula() != null)
				e.setAmountValue(eval(e.getAmountFormula()));
			eval(e.getUncertainty());
		} catch (Exception ex) {
			Error.showBox(Messages.FormulaEvaluationFailed, ex.getMessage());
		}
	}

	public void eval(ImpactFactor f) {
		try {
			if (f.getFormula() != null)
				f.setValue(eval(f.getFormula()));
			eval(f.getUncertainty());
		} catch (Exception e) {
			Error.showBox(Messages.FormulaEvaluationFailed, e.getMessage());
		}
	}

	private void eval(Uncertainty u) throws InterpreterException {
		if (u == null)
			return;
		if (u.getParameter1Formula() != null)
			u.setParameter1Value(eval(u.getParameter1Formula()));
		if (u.getParameter2Formula() != null)
			u.setParameter2Value(eval(u.getParameter2Formula()));
		if (u.getParameter3Formula() != null)
			u.setParameter3Value(eval(u.getParameter3Formula()));
	}

	/**
	 * Evaluates the expression in the context of this process.
	 */
	public double eval(String expression) throws InterpreterException {
		Scope scope = interpreter.getScope(editor.getModel().getId());
		return scope.eval(expression);
	}

}
