package org.openlca.app.results.regionalized;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.expressions.Scope;
import org.openlca.geo.parameter.ParameterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FactorCalculator {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ParameterSet regioParams;
	private Map<String, Double> inputParams = new HashMap<>();
	private Map<String, String> calcParams = new HashMap<>();

	FactorCalculator(ParameterSet inputParams, IDatabase db,
			CalculationSetup setup) {
		this.regioParams = inputParams;
		initContext(db, setup);
	}

	private void initContext(IDatabase db, CalculationSetup setup) {
		// order of the initialization is important
		ParameterDao dao = new ParameterDao(db);
		addToContext(dao.getGlobalParameters());
		for (ParameterRedef redef : setup.parameterRedefs) {
			if (redef.contextId == null) {
				inputParams.put(redef.name, redef.value);
			}
		}
		if (setup.impactMethod != null) {
			ImpactMethodDao methodDao = new ImpactMethodDao(db);
			ImpactMethod method = methodDao.getForId(setup.impactMethod.id);
			addToContext(method.parameters);
		}
	}

	private void addToContext(List<Parameter> params) {
		for (Parameter p : params) {
			if (p.isInputParameter) {
				inputParams.put(p.name, p.value);
			} else {
				calcParams.put(p.name, p.formula);
			}
		}
	}

	Map<FlowDescriptor, Double> calculate(ImpactCategory category, long locationId) {
		Scope scope = buildScope(locationId);
		Map<FlowDescriptor, Double> result = new HashMap<>();
		for (ImpactFactor factor : category.impactFactors) {
			FlowDescriptor flow = Descriptors.toDescriptor(factor.flow);
			if (factor.formula == null) {
				result.put(flow, factor.value);
			} else {
				result.put(flow, eval(factor, scope));
			}
		}
		return result;
	}

	private Scope buildScope(long locationId) {
		Scope scope = new FormulaInterpreter().getGlobalScope();
		// again: the order of filling the scope is important
		for (Entry<String, String> param : calcParams.entrySet()) {
			scope.bind(param.getKey(), param.getValue());
		}
		for (Entry<String, Double> param : inputParams.entrySet()) {
			if (param.getValue() == null)
				continue;
			scope.bind(param.getKey(), param.getValue().toString());
		}
		Map<String, Double> regioMap = regioParams.get(locationId);
		for (Entry<String, Double> param : regioMap.entrySet()) {
			if (param.getValue() == null)
				continue;
			scope.bind(param.getKey(), param.getValue().toString());
		}
		return scope;
	}

	private double eval(ImpactFactor factor, Scope scope) {
		try {
			return scope.eval(factor.formula);
		} catch (InterpreterException e) {
			log.error("Error evaluating formula " + factor.formula, e);
		}
		return 0;
	}
}
