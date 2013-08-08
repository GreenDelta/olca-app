package org.openlca.core.application.evaluation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.math.FormulaEvaluator;
import org.openlca.core.math.FormulaParseException;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Expression;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.openlca.core.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls the evaluation parameters. Listens to registered expressions and
 * reevaluates them if formulas or names are changed
 * 
 * @author Sebastian Greve
 * 
 */
public class EvaluationController implements PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private IDatabase database;
	private FormulaEvaluator evaluator = new FormulaEvaluator();
	private List<EvaluationListener> listeners = new ArrayList<>();
	private List<Parameter> parameters = new ArrayList<>();
	private Map<String, Expression> expressions = new HashMap<>();

	public EvaluationController(IDatabase database) {
		this.database = database;
	}

	public void addEvaluationListener(EvaluationListener listener) {
		listeners.add(listener);
	}

	public void evaluate() {
		List<Parameter> params = new ArrayList<>();
		params.addAll(parameters);
		addGlobalParams(params);
		for (String id : expressions.keySet()) {
			Parameter parameter = new Parameter(id, expressions.get(id),
					ParameterType.UNSPECIFIED, null);
			parameter.setName("e" + id.replace("-", "")); // TODO: this could
															// fail if the ID is
															// not an UUID
			params.add(parameter);
		}
		log.trace("evaluate {} expressions and parameters", params.size());
		evaluate(params);
	}

	private void addGlobalParams(List<Parameter> parameters) {
		try {
			ParameterDao dao = new ParameterDao(database.getEntityFactory());
			for (Parameter parameter : dao
					.getAllForType(ParameterType.DATABASE)) {
				if (!parameters.contains(parameter))
					parameters.add(parameter);
			}
		} catch (Exception e) {
			log.error("Could not load database parameters", e);
		}
	}

	private void evaluate(List<Parameter> parameters) {
		try {
			evaluator.evaluate(parameters);
			for (EvaluationListener listener : listeners)
				listener.evaluated();
		} catch (FormulaParseException e) {
			for (EvaluationListener listener : listeners)
				listener.error(e);
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		Object source = evt.getSource();
		String prop = evt.getPropertyName();
		if (source instanceof Parameter && prop.equals("name")
				|| prop.equals("formula")) {
			evaluate();
		} else if (source instanceof Expression && prop.equals("formula")) {
			String newFormula = evt.getNewValue().toString();
			try {
				Double newValue = Double.parseDouble(newFormula);
				Expression e = (Expression) source;
				e.setValue(newValue);
			} catch (NumberFormatException e) {
				evaluate();
			}
		}
	}

	public void resisterProcess(Process process) {
		for (Exchange exchange : process.getExchanges())
			registerExchange(exchange);
	}

	public void registerExchange(Exchange e) {
		if (e == null)
			return;
		String id = e.getId();
		registerExpression(id, e.getResultingAmount());
		if (e.getUncertaintyParameter1() != null)
			registerExpression(id + "u1", e.getUncertaintyParameter1());
		if (e.getUncertaintyParameter2() != null)
			registerExpression(id + "u2", e.getUncertaintyParameter2());
		if (e.getUncertaintyParameter3() != null)
			registerExpression(id + "u3", e.getUncertaintyParameter3());

	}

	private void registerExpression(String id, Expression expression) {
		if (id == null || expression == null)
			return;
		Expression old = expressions.get(id);
		if (old == expression) // no change
			return;
		if (old != null) {
			expressions.remove(id);
			old.removePropertyChangeListener(this);
		}
		expressions.put(id, expression);
		expression.addPropertyChangeListener(this);
	}

	public void unregisterExchange(Exchange e) {
		if (e == null)
			return;
		String id = e.getId();
		unregisterExpression(id);
		unregisterExpression(id + "u1");
		unregisterExpression(id + "u2");
		unregisterExpression(id + "u3");
	}

	private void unregisterExpression(String id) {
		if (id == null)
			return;
		Expression expression = expressions.remove(id);
		if (expression != null)
			expression.removePropertyChangeListener(this);
	}

	public void registerParameter(Parameter parameter) {
		if (parameter != null) {
			parameters.add(parameter);
			parameter.addPropertyChangeListener(this);
		}
	}

	public void removeEvaluationListener(EvaluationListener listener) {
		listeners.remove(listener);
	}

	public void unregisterParameter(Parameter parameter) {
		if (parameter != null) {
			parameter.removePropertyChangeListener(this);
			parameters.remove(parameter);
		}
	}

}
