/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.views.navigator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;
import org.openlca.core.application.Messages;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.AdminInfo;
import org.openlca.core.model.AllocationFactor;
import org.openlca.core.model.AllocationMethod;
import org.openlca.core.model.Category;
import org.openlca.core.model.Copyable;
import org.openlca.core.model.Exchange;
import org.openlca.core.model.LCIACategory;
import org.openlca.core.model.LCIAFactor;
import org.openlca.core.model.LCIAMethod;
import org.openlca.core.model.ModelingAndValidation;
import org.openlca.core.model.Process;
import org.openlca.core.model.Technology;
import org.openlca.core.model.Time;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This action creates a copy of the given category or model component and
 * inserts it into the database
 * 
 * @author Sebastian Greve
 * 
 */
public class Copier {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Category category;
	private String categoryId;
	private final IDatabase database;
	private String error = null;
	private IModelComponent modelComponent;
	private Category newParent;

	public Copier(Category category, IDatabase database, Category newParent) {
		this.category = category;
		this.database = database;
		this.newParent = newParent;
	}

	public Copier(IModelComponent modelComponent, IDatabase database,
			String categoryId) {
		this.modelComponent = modelComponent;
		this.database = database;
		this.categoryId = categoryId;
	}

	private IModelComponent[] collectModelComponents() {
		final List<IModelComponent> components = new ArrayList<>();
		// create query
		String query = "SELECT t FROM "
				+ category.getComponentClass().substring(
						category.getComponentClass().lastIndexOf('.') + 1)
				+ " t WHERE t.categoryId = '" + category.getId() + "'";
		final Queue<Category> categories = new LinkedList<>();

		// for each child category
		for (final Category child : category.getChildCategories()) {
			// add to list
			categories.add(child);
		}

		// while categories left
		while (!categories.isEmpty()) {
			// actual category
			final Category next = categories.poll();

			// add OR to query
			query += " OR t.categoryId = '" + next.getId() + "'";

			// for each child category
			for (final Category child : next.getChildCategories()) {
				// add to list
				categories.add(child);
			}
		}
		// query objects
		final Object[] objects = database.query(query);

		// for each model component
		for (final Object object : objects) {
			// add to list
			components.add((IModelComponent) object);
		}
		return components.toArray(new IModelComponent[components.size()]);
	}

	private void copyAdministrativeInformation(Process copy)
			throws DataProviderException {
		AdminInfo adminInfoCopy = database.select(AdminInfo.class,
				modelComponent.getId()).copy();
		adminInfoCopy.setId(copy.getId());
		database.insert(adminInfoCopy);
	}

	private void copyAllocationInformation(Process copy,
			Map<String, String> oldExchangeToNew) {
		copy.clearAllocationFactors();

		for (Exchange e : ((Process) modelComponent).getExchanges()) {
			Exchange eCopy = copy.getExchange(oldExchangeToNew.get(e.getId()));
			for (AllocationFactor factor : e.getAllocationFactors()) {
				AllocationFactor fCopy = new AllocationFactor();
				fCopy.setId(UUID.randomUUID().toString());
				fCopy.setValue(factor.getValue());
				fCopy.setProductId(oldExchangeToNew.get(factor.getProductId()));
				eCopy.add(fCopy);
			}
		}
	}

	/**
	 * Creates and inserts the copy of the category
	 * 
	 * @throws DataProviderException
	 */
	private void copyCategory() throws DataProviderException {
		// copy category
		final Category copy = category.copy();
		copy.setParentCategory(newParent);
		newParent.add(copy);

		// insert copy and update parent category
		database.insert(copy);
		database.refresh(newParent);

		// get model components to copy
		final IModelComponent[] modelComponentsToCopy = collectModelComponents();

		// for each model component
		for (final IModelComponent modelComponent : modelComponentsToCopy) {
			this.modelComponent = modelComponent;
			// get identic category in the copy
			categoryId = getIdenticCategory(category, copy,
					modelComponent.getCategoryId()).getId();
			// copy model component
			copyModelComponent();
		}
	}

	/**
	 * Copies the LCIA categories and factors of a method
	 * 
	 * @param copy
	 *            The LCIA method copy
	 * @throws DataProviderException
	 */
	private void copyLCIACategories(final LCIAMethod copy)
			throws DataProviderException {
		final LCIACategory[] cats = copy.getLCIACategories();

		// for each LCIA category
		for (final LCIACategory cat : cats) {
			// remove from copy
			copy.remove(cat);
		}

		// each original LCIA category
		for (final LCIACategory category : ((LCIAMethod) modelComponent)
				.getLCIACategories()) {
			// copy category
			final LCIACategory catCopy = category.copy();

			// get LCIA factors of the copy
			final LCIAFactor[] factors = catCopy.getLCIAFactors();

			// for each factor
			for (final LCIAFactor factor : factors) {
				// remove from copy
				catCopy.remove(factor);
			}

			// for each LCIA factor of the original category
			for (final LCIAFactor factor : category.getLCIAFactors()) {
				// copy
				final LCIAFactor fCopy = factor.copy();
				catCopy.add(fCopy);
			}
			copy.add(catCopy);
		}
		// insert copy
		database.insert(copy);
	}

	private void copyModelComponent() throws DataProviderException {
		modelComponent = database.select(modelComponent.getClass(),
				modelComponent.getId());
		IModelComponent copy = (IModelComponent) ((Copyable<?>) modelComponent)
				.copy();
		copy.setName(copy.getName() + " (copy)");
		copy.setCategoryId(categoryId);

		// copy procedure
		if (!(copy instanceof UnitGroup)) {
			if (copy instanceof LCIAMethod) {
				copyLCIACategories((LCIAMethod) copy);
			} else if (copy instanceof Process) {
				copyProcessInformation((Process) copy);
			} else {
				database.insert(copy);
			}
		} else {
			// unit groups cannot be copied
			error = Messages.Copier_CannotCopyUG;
		}
	}

	private void copyModelingAndValidationInformation(Process copy)
			throws DataProviderException {
		ModelingAndValidation modelingAndValidation = database.select(
				ModelingAndValidation.class, modelComponent.getId());
		ModelingAndValidation modelingAndValidationCopy = modelingAndValidation
				.copy();
		modelingAndValidationCopy.setId(copy.getId());
		database.insert(modelingAndValidationCopy);
	}

	private void copyProcessInformation(Process copy)
			throws DataProviderException {
		Map<String, String> oldExchangeToNew = copyUncertaintyInformation(copy);
		if (copy.getAllocationMethod() != null
				&& copy.getAllocationMethod() != AllocationMethod.None) {
			copyAllocationInformation(copy, oldExchangeToNew);
		}
		database.insert(copy);
		copyAdministrativeInformation(copy);
		copyModelingAndValidationInformation(copy);
		copyTimeInformation(copy);
		copyTechnologyInformation(copy);
	}

	private void copyTechnologyInformation(Process copy)
			throws DataProviderException {
		Technology technology = database.select(Technology.class,
				modelComponent.getId());
		Technology technologyCopy = technology.copy();
		technologyCopy.setId(copy.getId());
		database.insert(technologyCopy);
	}

	private void copyTimeInformation(final Process copy)
			throws DataProviderException {
		Time time = database.select(Time.class, modelComponent.getId());
		Time timeCopy = time.copy();
		timeCopy.setId(copy.getId());
		database.insert(timeCopy);
	}

	private Map<String, String> copyUncertaintyInformation(Process copy) {
		Exchange[] exchanges = copy.getExchanges();
		for (Exchange exchange : exchanges) {
			copy.remove(exchange);
		}

		final Map<String, String> oldExchangeToNew = new HashMap<>();
		for (Exchange e : ((Process) modelComponent).getExchanges()) {
			Exchange eCopy = new Exchange(copy.getId());
			eCopy.setId(UUID.randomUUID().toString());
			eCopy.setAvoidedProduct(e.isAvoidedProduct());
			eCopy.setFlow(e.getFlow());
			eCopy.setFlowPropertyFactor(e.getFlowPropertyFactor());
			eCopy.setInput(e.isInput());
			eCopy.getResultingAmount().setValue(
					e.getResultingAmount().getValue());
			eCopy.getResultingAmount().setFormula(
					e.getResultingAmount().getFormula());
			eCopy.setUnit(e.getUnit());
			oldExchangeToNew.put(e.getId(), eCopy.getId());
			if (e.equals(((Process) modelComponent).getQuantitativeReference())) {
				copy.setQuantitativeReference(eCopy);
			}
			copy.add(eCopy);
		}
		return oldExchangeToNew;
	}

	/**
	 * Searches in the copy of the original category a specific category
	 * 
	 * @param original
	 *            The original category
	 * @param copy
	 *            The copy
	 * @param categoryIdToFind
	 *            The id of the category to find
	 * @return The category in the copy equal to the category in the original
	 */
	private Category getIdenticCategory(final Category original,
			final Category copy, final String categoryIdToFind) {
		Category identic = null;
		if (original.getId().equals(categoryIdToFind)) {
			identic = copy;
		} else {
			int i = 0;
			while (identic == null && i < original.getChildCategories().length) {
				identic = getIdenticCategory(original.getChildCategories()[i],
						copy.getChildCategories()[i], categoryIdToFind);
				i++;
			}
		}
		return identic;
	}

	/**
	 * Copies the model component
	 */
	public void copy() {
		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {

						@Override
						public void run(final IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							try {
								monitor.beginTask(Messages.Copier_Copying,
										IProgressMonitor.UNKNOWN);
								if (modelComponent != null
										&& modelComponent instanceof Copyable) {
									copyModelComponent();
								} else if (category != null) {
									copyCategory();
								}
								monitor.done();
							} catch (final Exception e) {
								throw new InvocationTargetException(e);
							}

						}
					});
		} catch (final Exception e) {
			log.error("Copy model component failed", e);
		}
	}

	public String getError() {
		return error;
	}

}
