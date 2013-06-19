/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.evaluation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.views.ModelEditorInput;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.math.FormulaEvaluator;
import org.openlca.core.math.FormulaParseException;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.Expression;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates all parameters and expressions on the database
 * 
 * @author Sebastian Greve
 * 
 */
public class ParametrizableComponentUpdater {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Updates the parameters and expression on the database
	 * 
	 * @param databaseParameters
	 *            The database wide parameters
	 * @param database
	 *            The database
	 * @param sortMode
	 *            The sort direction
	 * @return true if update was successful, false otherwise
	 */
	public boolean update(final Collection<Parameter> databaseParameters,
			final IDatabase database, final int sortMode) {
		// get sort direction
		final String value = ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION
				.getValue(database.getUrl());
		if (!value.equals(sortMode + "")) {
			updateSortDirection(database, sortMode + "");
		}
		final Map<String, List<Parameter>> idToParameters = new HashMap<>();
		final Map<String, Exchange> idToExchange = new HashMap<>();
		final List<LCIAMethod> methods = new ArrayList<>();

		// load parameters and expressions
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {

						@Override
						public void run(final IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							initializeParameters(database, databaseParameters,
									idToParameters);
							initializeExchanges(database, databaseParameters,
									idToParameters, idToExchange);
						}
					});
		} catch (final Exception e) {
			log.error("Loading parameters and expressions failed", e);
		}

		try {
			evaluate(database, idToParameters);
			List<IModelComponent> components = collectOpenedComponents();
			update(database, idToExchange, idToParameters);
			for (LCIAMethod method : methods) {
				update(database, method);
			}
			updateEditors(idToExchange, idToParameters, components);
			if (!value.equals(sortMode + "")) {
				updateSortDirection(database, value);
			}
			return false;
		} catch (Exception e) {
			log.warn("Formula evaluation failed " + e.getMessage(), e);
			return true;
		}
	}

	private List<IModelComponent> collectOpenedComponents() {
		final List<IModelComponent> components = new ArrayList<>();
		final IEditorReference[] references = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.getEditorReferences();
		// for each editor reference
		for (final IEditorReference reference : references) {
			try {
				IEditorInput input = reference.getEditorInput();
				if (input instanceof ModelEditorInput) {
					ModelEditorInput modelInput = (ModelEditorInput) input;
					BaseDescriptor d = modelInput.getDescriptor();
					ModelType t = d.getModelType();
					if (t == ModelType.PROCESS || t == ModelType.PRODUCT_SYSTEM
							|| t == ModelType.IMPACT_METHOD) {
						IModelComponent model = (IModelComponent) modelInput
								.getDatabase().createDao(t.getModelClass())
								.getForId(d.getId());
						components.add(model);
					}
				}
			} catch (Exception e) {
				log.error("Add model component failed", e);
			}
		}
		return components;
	}

	private void evaluate(IDatabase database,
			Map<String, List<Parameter>> idToParameters)
			throws FormulaParseException {
		for (List<Parameter> parameters : idToParameters.values()) {
			FormulaEvaluator evaluator = new FormulaEvaluator();
			evaluator.evaluate(parameters);
		}
	}

	private void initializeExchanges(final IDatabase database,
			final Collection<Parameter> databaseParameters,
			final Map<String, List<Parameter>> idToParameters,
			final Map<String, Exchange> idToExchange) {
		try {
			// load parametrized exchanges
			final Map<String, Object> properties = new HashMap<>();
			properties.put("parametrized", "1");
			final List<Exchange> objects = database.selectAll(Exchange.class,
					properties);
			// for each exchange
			for (final Object object : objects) {
				final Exchange exchange = (Exchange) object;
				idToExchange.put(exchange.getId(), exchange);
				List<Parameter> parameters = idToParameters.get(exchange
						.getOwnerId());
				if (parameters == null) {
					parameters = new ArrayList<>();
					parameters.addAll(databaseParameters);
				}
				// create representing parameter
				final Parameter parameter = new Parameter(exchange.getId(),
						exchange.getResultingAmount(),
						ParameterType.UNSPECIFIED, null);
				parameter.setName("e" + exchange.getId().replace("-", ""));

				// uncertainty
				if (exchange.getUncertaintyParameter1() != null) {
					final Parameter pp1 = new Parameter("e"
							+ exchange.getId().replace("-", "") + "u1",
							exchange.getUncertaintyParameter1(),
							ParameterType.UNSPECIFIED, null);
					pp1.setName(pp1.getId());
					parameters.add(pp1);
				}
				if (exchange.getUncertaintyParameter2() != null) {
					final Parameter pp2 = new Parameter("e"
							+ exchange.getId().replace("-", "") + "u2",
							exchange.getUncertaintyParameter2(),
							ParameterType.UNSPECIFIED, null);
					pp2.setName(pp2.getId());
					parameters.add(pp2);
				}
				if (exchange.getUncertaintyParameter3() != null) {
					final Parameter pp3 = new Parameter("e"
							+ exchange.getId().replace("-", "") + "u3",
							exchange.getUncertaintyParameter3(),
							ParameterType.UNSPECIFIED, null);
					pp3.setName(pp3.getId());
					parameters.add(pp3);
				}

				parameters.add(parameter);
				idToParameters.put(exchange.getOwnerId(), parameters);
			}
		} catch (final Exception e) {
			log.error("Initializing exchange map failed", e);
		}
	}

	/**
	 * Initializes the parameters map with all the parameters from the database
	 * 
	 * @param database
	 *            The database
	 * @param databaseParameters
	 *            The database wide parameters
	 * @param idToParameters
	 *            A map between owner id and the parameters of the owner
	 */
	private void initializeParameters(final IDatabase database,
			final Collection<Parameter> databaseParameters,
			final Map<String, List<Parameter>> idToParameters) {
		try {
			ParameterDao dao = new ParameterDao(database.getEntityFactory());
			for (Parameter parameter : dao.getAll()) {
				// if not database parameter
				if (parameter.getType() != ParameterType.DATABASE) {
					List<Parameter> parameters = idToParameters.get(parameter
							.getOwnerId());
					if (parameters == null) {
						parameters = new ArrayList<>();
						parameters.addAll(databaseParameters);
					}
					parameters.add(parameter);
					idToParameters.put(parameter.getOwnerId(), parameters);
				}
			}
		} catch (final Exception e) {
			log.error("Initializing parameters failed", e);
		}
	}

	private void update(IDatabase database, Map<String, Exchange> idToExchange,
			Map<String, List<Parameter>> idToParameters) {
		for (List<Parameter> parameters : idToParameters.values()) {
			for (Parameter parameter : parameters) {
				if (parameter.getType() != ParameterType.UNSPECIFIED)
					update(database, parameter);
				else {
					Exchange exchange = idToExchange.get(parameter.getId());
					if (exchange != null)
						update(database, exchange);
				}
			}
		}
	}

	private void update(IDatabase database, Object object) {
		try {
			database.refresh(object);
		} catch (final DataProviderException e) {
			log.error("Update failed for " + object, e);
		}
	}

	/**
	 * Updates the components currently opened in an editor
	 * 
	 * @param idToExchange
	 *            A map between exchange.id and the corresponding exchange
	 * @param idToParameters
	 *            A map between owner id and the parameters of the owner
	 * @param openComponents
	 *            A list of components currently opened in an editor
	 */
	private void updateEditors(final Map<String, Exchange> idToExchange,
			final Map<String, List<Parameter>> idToParameters,
			final List<IModelComponent> openComponents) {
		for (final IModelComponent component : openComponents) {
			if (component instanceof ProductSystem) {
				updateProductSystem((ProductSystem) component, idToExchange,
						idToParameters);
			} else if (component instanceof Process) {
				updateProcess((Process) component, idToExchange, idToParameters);
			}
		}
	}

	/**
	 * Update the given process parameters and exchanges
	 * 
	 * @param process
	 *            The process to update
	 * @param idToExchange
	 *            A map between exchange.id and the corresponding exchange
	 * @param idToParameters
	 *            A map between owner id and the parameters of the owner
	 */
	private void updateProcess(final Process process,
			final Map<String, Exchange> idToExchange,
			final Map<String, List<Parameter>> idToParameters) {
		// update exchanges expressions
		for (final Exchange exchange : process.getExchanges()) {
			if (idToExchange.containsKey(exchange.getId())) {
				exchange.getResultingAmount().setValue(
						idToExchange.get(exchange.getId()).getResultingAmount()
								.getValue());
			}
		}
		final Map<String, Expression> idToValue = new HashMap<>();
		final List<Parameter> actual = idToParameters.get(process.getId());
		if (actual != null) {
			// collect parameter expressions
			for (final Parameter parameter : actual) {
				idToValue.put(parameter.getId(), parameter.getExpression());
			}
		}
		for (final Parameter parameter : process.getParameters()) {
			// update parameters
			final Expression expression = idToValue.get(parameter.getId());
			if (expression != null) {
				parameter.getExpression().setValue(expression.getValue());
			}
		}
	}

	/**
	 * Update the given product system parameters and parameters and exchanges
	 * of all it's processes
	 * 
	 * @param productSystem
	 *            The process to update
	 * @param idToExchange
	 *            A map between exchange.id and the corresponding exchange
	 * @param idToParameters
	 *            A map between owner id and the parameters of the owner
	 */
	private void updateProductSystem(final ProductSystem productSystem,
			final Map<String, Exchange> idToExchange,
			final Map<String, List<Parameter>> idToParameters) {
		for (final Process process : productSystem.getProcesses()) {
			// update exchange resulting amounts
			for (final Exchange exchange : process.getExchanges()) {
				if (idToExchange.containsKey(exchange.getId())) {
					exchange.getResultingAmount().setValue(
							idToExchange.get(exchange.getId())
									.getResultingAmount().getValue());
				}
			}
		}
		final Map<String, Expression> idToValue = new HashMap<>();
		final List<Parameter> actual = idToParameters
				.get(productSystem.getId());
		if (actual != null) {
			// collect parameter expressions
			for (final Parameter parameter : actual) {
				idToValue.put(parameter.getId(), parameter.getExpression());
			}
		}
		for (final Parameter parameter : productSystem.getParameters()) {
			// update parameters
			final Expression expression = idToValue.get(parameter.getId());
			if (expression != null) {
				parameter.getExpression().setValue(expression.getValue());
			}
		}
	}

	/**
	 * Saves the sort direction into the properties file
	 * 
	 * @param database
	 *            The database
	 * @param sortMode
	 *            The new sort direction
	 */
	private void updateSortDirection(final IDatabase database,
			final String sortMode) {
		ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION.setValue(sortMode,
				database.getUrl());
	}
}
