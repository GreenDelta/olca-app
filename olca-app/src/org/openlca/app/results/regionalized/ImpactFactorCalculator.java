package org.openlca.app.results.regionalized;

import java.util.HashMap;
import java.util.Map;

import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ImpactFactor;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.InterpreterException;
import org.openlca.expressions.Scope;
import org.openlca.geo.parameter.ParameterSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

public class ImpactFactorCalculator {

	private Logger log = LoggerFactory.getLogger(getClass());
	private ParameterSet parameterSet;

	private Map<FlowDescriptor, Double> result;
	private Scope scope;

	public ImpactFactorCalculator(ParameterSet parameterSet) {
		this.parameterSet = parameterSet;
	}

	public Map<FlowDescriptor, Double> calculate(ImpactCategory category,
			long locationId) {
		setup(locationId);
		for (ImpactFactor factor : category.getImpactFactors())
			put(factor);
		return result;
	}

	private void setup(long locationId) {
		scope = new FormulaInterpreter().createScope(1);
		Map<String, Double> parameters = parameterSet.getFor(locationId);
		for (String name : parameters.keySet())
			scope.bind(name, Double.toString(parameters.get(name)));
		result = new HashMap<>();
	}

	private void put(ImpactFactor factor) {
		FlowDescriptor flow = Descriptors.toDescriptor(factor.getFlow());
		if (!isParametrized(factor))
			result.put(flow, factor.getValue());
		else
			result.put(flow, calculate(factor));
	}

	private double calculate(ImpactFactor factor) {
		try {
			return scope.eval(factor.getFormula());
		} catch (InterpreterException e) {
			log.error("Error evaluating formula " + factor.getFormula(), e);
		}
		return 0;
	}

	private boolean isParametrized(ImpactFactor factor) {
		try {
			if (Strings.isNullOrEmpty(factor.getFormula()))
				return false;
			Double.parseDouble(factor.getFormula());
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}

}
