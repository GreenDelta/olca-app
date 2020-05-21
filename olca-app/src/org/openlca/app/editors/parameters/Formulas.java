package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterizedEntity;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some helper methods for evaluating formulas in the editors.
 */
public class Formulas {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final List<String> errors = new ArrayList<>();
	private final IDatabase db;

	private Formulas(IDatabase db) {
		this.db = db;
	}

	public static List<String> eval(IDatabase db, Process process) {
		if (db == null || process == null)
			return Collections.emptyList();
		return new Formulas(db).eval(process);
	}

	public static List<String> eval(IDatabase db, ImpactCategory impact) {
		if (db == null || impact == null)
			return Collections.emptyList();
		return new Formulas(db).eval(impact);
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
			log.warn("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private List<String> eval(Process p) {
		try {
			var scope = createScope(db, p);
			evalParams(p.parameters, scope);
			for (var e : p.exchanges) {
				if (Strings.notEmpty(e.formula)) {
					e.amount = eval(e.formula, scope);
				}
				eval(e.uncertainty, scope);
				if (Strings.notEmpty(e.costFormula)) {
					e.costs = eval(e.costFormula, scope);
				}
			}
			for (var af : p.allocationFactors) {
				if (Strings.notEmpty(af.formula)) {
					af.value = eval(af.formula, scope);
				}
			}
		} catch (Exception e) {
			log.warn("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private List<String> eval(ImpactCategory c) {
		try {
			var s = createScope(db, c);
			evalParams(c.parameters, s);
			for (var f : c.impactFactors) {
				if (Strings.notEmpty(f.formula)) {
					f.value = eval(f.formula, s);
				}
				eval(f.uncertainty, s);
			}
		} catch (Exception e) {
			log.warn("unexpected error in formula evaluation", e);
		}
		return errors;
	}

	private void evalParams(List<Parameter> params, Scope s) {
		for (var param : params) {
			if (param.isInputParameter)
				continue;
			param.value = eval(param.formula, s);
		}
	}

	private void eval(Uncertainty u, Scope s) {
		if (u == null)
			return;
		if (Strings.notEmpty(u.formula1)) {
			u.parameter1 = eval(u.formula1, s);
		}
		if (Strings.notEmpty(u.formula2)) {
			u.parameter2 = eval(u.formula2, s);
		}
		if (Strings.notEmpty(u.formula3)) {
			u.parameter3 = eval(u.formula3, s);
		}
	}

	private double eval(String formula, Scope s) {
		if (Strings.nullOrEmpty(formula) || s == null)
			return 0;
		try {
			double val = s.eval(formula);
			log.trace("evaluated: {} -> {}", formula, val);
			return val;
		} catch (Exception e) {
			log.warn("failed to evaluate " + formula, e);
			errors.add(formula);
			return 0;
		}
	}

	/**
	 * Creates an evaluation scope for the given entity with bindings to local
	 * parameters and a reference to a global scope which contains the database
	 * parameters.
	 */
	public static Scope createScope(IDatabase db, ParameterizedEntity entity) {
		var interpreter = new FormulaInterpreter();
		var global = interpreter.getGlobalScope();
		if (db == null || entity == null)
			return global;
		for (var param : new ParameterDao(db).getGlobalParameters()) {
			bind(param, global);
		}
		var localScope = interpreter.createScope(entity.id);
		for (var param : entity.parameters) {
			bind(param, localScope);
		}
		return localScope;
	}

	private static void bind(Parameter param, Scope scope) {
		if (param == null || scope == null)
			return;
		if (param.isInputParameter || Strings.nullOrEmpty(param.formula)) {
			scope.bind(param.name, param.value);
		} else {
			scope.bind(param.name, param.formula);
		}
	}

}
