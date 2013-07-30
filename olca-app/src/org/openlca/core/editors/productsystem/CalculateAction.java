/*******************************************************************************
 * Copyright (c) 2007 - 2012 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.math.FormulaEvaluator;
import org.openlca.core.math.FormulaParseException;
import org.openlca.core.math.MatrixSolver;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.model.results.InventoryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates results for a product system
 * 
 * @author Sebastian Greve
 * 
 */
class CalculateAction {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	public InventoryResult calculate(ProductSystem productSystem,
			IDatabase database, MatrixSolver calculator) throws Exception {
		log.trace("start calculation");
		evaluateParameters(productSystem, database);
		InventoryResult result = calculator.calculate(productSystem);
		log.trace("calculation done");
		return result;
	}

	public AnalysisResult calculateAggregatedProcessResults(
			ProductSystem productSystem, IDatabase database,
			MatrixSolver calculator) throws Exception {
		evaluateParameters(productSystem, database);
		AnalysisResult results = calculator.analyse(productSystem);
		return results;
	}

	// TODO: better we do this in the solver
	private void evaluateParameters(ProductSystem productSystem,
			IDatabase database) {
		log.trace("call prepare");
		if (productSystem.getParameters().size() > 0) {
			try {
				List<Parameter> databaseParameters = new ParameterDao(
						database)
						.getAllForType(ParameterType.DATABASE);
				for (Process process : productSystem.getProcesses()) {
					updateExpressions(productSystem, database, process,
							databaseParameters);
				}
			} catch (Exception e) {
				log.error("updating database parameters failed", e);
			}
		}
	}

	private void updateExpressions(ProductSystem productSystem,
			IDatabase database, Process process,
			Collection<Parameter> databaseParameters)
			throws FormulaParseException {
		log.trace("call updateExpressions");
		boolean isParametrized = false;
		List<Parameter> parameters = new ArrayList<>();
		List<Parameter> exchangeParameterDummies = new ArrayList<>();
		for (Exchange exchange : process.getExchanges()) {
			if (exchange.isParametrized()) {
				Parameter parameter = new Parameter(exchange.getId(),
						exchange.getResultingAmount(),
						ParameterType.UNSPECIFIED, null);
				parameter.setName("e" + exchange.getId().replace("-", ""));
				parameters.add(parameter);
				exchangeParameterDummies.add(parameter);
				isParametrized = true;
			}
		}
		if (isParametrized) {
			FormulaEvaluator evaluator = new FormulaEvaluator();
			parameters.addAll(databaseParameters);

			for (Parameter param : productSystem.getParameters()) {
				parameters.add(param);
			}
			for (Parameter param : process.getParameters()) {
				parameters.add(param);
			}

			evaluator.evaluate(parameters);

			for (Parameter p : exchangeParameterDummies) {
				process.getExchange(p.getId()).getResultingAmount()
						.setValue(p.getExpression().getValue());
			}
		}

	}
}
