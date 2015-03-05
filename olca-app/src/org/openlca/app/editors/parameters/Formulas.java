package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some helper methods for evaluating formulas in the editors.
 */
public class Formulas {

	private Logger log = LoggerFactory.getLogger(getClass());

	private List<String> errors = new ArrayList<>();
	private IDatabase db;

	private Formulas(IDatabase db) {
		this.db = db;
	}

	public static List<String> eval(IDatabase db, Process process) {
		if (db == null || process == null)
			return Collections.emptyList();
		return new Formulas(db).eval(process);
	}

	public static List<String> eval(IDatabase db, ImpactMethod method) {
		if (db == null || method == null)
			return Collections.emptyList();
		return new Formulas(db).eval(method);
	}

	public static List<String> eval(List<Parameter> params) {
		if (params == null)
			return Collections.emptyList();
		return new Formulas(null).evalGlobal(params);
	}

	private List<String> evalGlobal(List<Parameter> params) {
		try {
			FormulaInterpreter interpreter = new FormulaInterpreter();
			Scope scope = interpreter.getGlobalScope();
			for (Parameter p : params)
				bind(p, scope);
			evalParams(params, scope);
		} catch (Exception e) {
			log.error("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private List<String> eval(Process p) {
		try {
			Scope s = makeLocalScope(p.getParameters(), p.getId());
			evalParams(p.getParameters(), s);
			evalExchanges(p.getExchanges(), s);
		} catch (Exception e) {
			log.error("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private List<String> eval(ImpactMethod m) {
		try {
			Scope s = makeLocalScope(m.getParameters(), m.getId());
			evalParams(m.getParameters(), s);
			for (ImpactCategory ic : m.getImpactCategories())
				evalFactors(ic.getImpactFactors(), s);
		} catch (Exception e) {
			log.error("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private void evalParams(List<Parameter> params, Scope s) {
		for (Parameter param : params) {
			if (param.isInputParameter())
				continue;
			double val = eval(param.getFormula(), s);
			param.setValue(val);
		}
	}

	private void evalExchanges(List<Exchange> exchanges, Scope s) {
		for (Exchange e : exchanges) {
			if (e.getAmountFormula() != null)
				e.setAmountValue(eval(e.getAmountFormula(), s));
			eval(e.getUncertainty(), s);
		}
	}

	private void evalFactors(List<ImpactFactor> factors, Scope s) {
		for (ImpactFactor f : factors) {
			if (f.getFormula() != null)
				f.setValue(eval(f.getFormula(), s));
			eval(f.getUncertainty(), s);
		}
	}

	private void eval(Uncertainty u, Scope s) {
		if (u == null)
			return;
		if (u.getParameter1Formula() != null)
			u.setParameter1Value(eval(u.getParameter1Formula(), s));
		if (u.getParameter2Formula() != null)
			u.setParameter2Value(eval(u.getParameter2Formula(), s));
		if (u.getParameter3Formula() != null)
			u.setParameter3Value(eval(u.getParameter3Formula(), s));
	}

	private double eval(String formula, Scope s) {
		if (formula == null || formula.trim().isEmpty() || s == null)
			return 0;
		try {
			double val = s.eval(formula);
			log.trace("evaluated: {} -> {}", formula, val);
			return val;
		} catch (Exception e) {
			log.error("failed to evaluate " + formula, e);
			errors.add(formula);
			return 0;
		}
	}

	private Scope makeLocalScope(List<Parameter> params, long scopeId) {
		FormulaInterpreter interpreter = new FormulaInterpreter();
		ParameterDao dao = new ParameterDao(db);
		Scope globalScope = interpreter.getGlobalScope();
		for (Parameter p : dao.getGlobalParameters())
			bind(p, globalScope);
		Scope localScope = interpreter.createScope(scopeId);
		for (Parameter p : params)
			bind(p, localScope);
		return localScope;
	}

	private void bind(Parameter param, Scope scope) {
		if (param == null || scope == null)
			return;
		if (param.isInputParameter())
			scope.bind(param.getName(), Double.toString(param.getValue()));
		else
			scope.bind(param.getName(), param.getFormula());
	}

}
