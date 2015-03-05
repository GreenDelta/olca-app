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
	private long scopeId;

	private Formulas(long scopeId, IDatabase db) {
		this.scopeId = scopeId;
		this.db = db;
	}

	public static List<String> eval(IDatabase db, Process process) {
		if (db == null || process == null)
			return Collections.emptyList();
		return new Formulas(process.getId(), db).eval(process);
	}

	public static List<String> eval(IDatabase db, ImpactMethod method) {
		if (db == null || method == null)
			return Collections.emptyList();
		return new Formulas(method.getId(), db).eval(method);
	}

	private List<String> eval(Process p) {
		try {
			FormulaInterpreter i = makeInterpreter(p.getParameters());
			evalParams(p.getParameters(), i);
			evalExchanges(p.getExchanges(), i);
		} catch (Exception e) {
			log.error("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private List<String> eval(ImpactMethod m) {
		try {
			FormulaInterpreter i = makeInterpreter(m.getParameters());
			evalParams(m.getParameters(), i);
			for (ImpactCategory ic : m.getImpactCategories())
				evalFactors(ic.getImpactFactors(), i);
		} catch (Exception e) {
			log.error("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private void evalParams(List<Parameter> params, FormulaInterpreter i) {
		for (Parameter param : params) {
			if (param.isInputParameter())
				continue;
			double val = eval(param.getFormula(), i);
			param.setValue(val);
		}
	}

	private void evalExchanges(List<Exchange> exchanges, FormulaInterpreter i) {
		for (Exchange e : exchanges) {
			if (e.getAmountFormula() != null)
				e.setAmountValue(eval(e.getAmountFormula(), i));
			eval(e.getUncertainty(), i);
		}
	}

	private void evalFactors(List<ImpactFactor> factors, FormulaInterpreter i) {
		for (ImpactFactor f : factors) {
			if (f.getFormula() != null)
				f.setValue(eval(f.getFormula(), i));
			eval(f.getUncertainty(), i);
		}
	}

	private void eval(Uncertainty u, FormulaInterpreter i) {
		if (u == null)
			return;
		if (u.getParameter1Formula() != null)
			u.setParameter1Value(eval(u.getParameter1Formula(), i));
		if (u.getParameter2Formula() != null)
			u.setParameter2Value(eval(u.getParameter2Formula(), i));
		if (u.getParameter3Formula() != null)
			u.setParameter3Value(eval(u.getParameter3Formula(), i));
	}

	private double eval(String formula, FormulaInterpreter i) {
		if (formula == null || formula.trim().isEmpty() || i == null)
			return 0;
		try {
			double val = i.getScope(scopeId).eval(formula);
			log.trace("evaluated: {} -> {}", formula, val);
			return val;
		} catch (Exception e) {
			log.error("failed to evaluate " + formula, e);
			errors.add(formula);
			return 0;
		}
	}

	private FormulaInterpreter makeInterpreter(List<Parameter> params) {
		FormulaInterpreter interpreter = new FormulaInterpreter();
		ParameterDao dao = new ParameterDao(db);
		Scope globalScope = interpreter.getGlobalScope();
		for (Parameter p : dao.getGlobalParameters())
			bind(p, globalScope);
		Scope localScope = interpreter.createScope(scopeId);
		for (Parameter p : params)
			bind(p, localScope);
		return interpreter;
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
