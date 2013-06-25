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

import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.CategoryElement;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.ModelElement;
import org.openlca.core.application.navigation.Navigator;
import org.openlca.core.application.plugin.Activator;
import org.openlca.core.database.DataProviderException;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.modelprovider.IModelComponent;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The copy & paste manager caches cut and copied elements and pastes them
 * 
 * @author Sebastian Greve
 * 
 */
public class CopyPasteManager {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * The single instance
	 */
	private static CopyPasteManager instance;

	/**
	 * The cached elements
	 */
	private INavigationElement[] cache;

	/**
	 * Indicates the mode, if true elements were copied, if false elements were
	 * cut
	 */
	private boolean copyMode;

	/**
	 * Getter of the singleton instance
	 * 
	 * @return The singleton instance
	 */
	public static CopyPasteManager getInstance() {
		if (instance == null) {
			instance = new CopyPasteManager();
		}
		return instance;
	}

	/**
	 * Checks if the given element can be moved t o the given target element
	 * 
	 * @param element
	 *            The element to be moved
	 * @param targetElement
	 *            The target element
	 * @return Null if the element can be moved to the target, an error message
	 *         otherwise
	 */
	private String checkCategory(final CategoryElement element,
			final CategoryElement targetElement) {
		String error = null;
		// the dragged category
		final Category draggedCategory = (Category) element.getData();
		// the target category
		final Category targetCategory = (Category) targetElement.getData();

		// if component class does not match
		if (!draggedCategory.getComponentClass().equals(
				targetCategory.getComponentClass())) {
			error = Messages.NavigationDropAssistant_SameTypeError;
		}

		// if categories are the same
		if (draggedCategory.getId().equals(targetCategory.getId())) {
			error = Messages.NavigationDropAssistant_DropToItselfError;
		}

		// if databases does not match
		if (!element.getDatabase().equals(targetElement.getDatabase())) {
			error = Messages.NavigationDropAssistant_MoveIntoDatabaseError;
		}

		// if no error was detected so far
		if (error == null) {
			final Queue<Category> children = new LinkedList<>();
			// for each child category
			for (final Category child : draggedCategory.getChildCategories()) {
				children.add(child);
			}

			// while no error detected and child categories left
			// check if the category should be dropped on one of its children
			while (error == null && !children.isEmpty()) {
				// the actual child
				final Category child = children.poll();
				// child category equals target category
				if (child.getId().equals(targetCategory.getId())) {
					error = Messages.NavigationDropAssistant_ChildCategoryError;
				} else {
					// for each child of the child
					for (final Category childCategory : child
							.getChildCategories()) {
						// add to list
						children.add(childCategory);
					}
				}
			}
		}
		return error;
	}

	/**
	 * Checks if the given elements can be moved t o the given target element
	 * 
	 * @param elements
	 *            The elements to be moved
	 * @param targetElement
	 *            The target element
	 * @return Null if the elements can be moved to the target, an error message
	 *         otherwise
	 */
	private String checkElements(final INavigationElement[] elements,
			final CategoryElement targetElement) {
		String error = null;
		int i = 0;
		// check all element until an error occurs
		while (error == null && i < elements.length) {
			// the element to check next
			final INavigationElement o = elements[i];
			if (o instanceof CategoryElement) {
				// check category
				error = checkCategory((CategoryElement) o,
						targetElement);
			} else if (o instanceof ModelElement) {
				// check model component
				error = checkModelComponent((ModelElement) o,
						targetElement);
			}
			i++;
		}
		return error;
	}

	/**
	 * Checks if the given element can be moved t o the given target element
	 * 
	 * @param element
	 *            The element to be moved
	 * @param targetElement
	 *            The target element
	 * @return Null if the element can be moved to the target, an error message
	 *         otherwise
	 */
	private String checkModelComponent(final ModelElement element,
			final CategoryElement targetElement) {
		String error = null;
		// the model component
		final IModelComponent modelComponent = (IModelComponent) element
				.getData();
		// the target category
		final Category category = (Category) targetElement.getData();

		// check if component class of the category matches the component's
		// class
		if (!category.getComponentClass().equals(
				modelComponent.getClass().getCanonicalName())) {
			error = Messages.NavigationDropAssistant_WrongTypeError;
		}
		return error;
	}

	/**
	 * Copy the elements to the target category element
	 * 
	 * @param targetElement
	 *            The target category element
	 * @return An error message if any error occurred, null otherwise
	 */
	private String doIt(final CategoryElement targetElement) {
		// check the elements to be dropped
		String error = checkElements(cache, targetElement);
		if (error == null) {
			try {
				int i = 0;
				while (i < cache.length) {
					final Object elem = cache[i];
					i++;

					final IDatabase targetDatabase = targetElement
							.getDatabase();
					if (elem instanceof ModelElement) {
						final ModelElement mnElem = (ModelElement) elem;
						final IDatabase database = mnElem.getDatabase();
						if (database.equals(targetDatabase)) {
							if (copyMode) {
								final Copier copier = new Copier(
										(IModelComponent) mnElem.getData(),
										database,
										((Category) targetElement.getData())
												.getId());
								copier.copy();
								error = copier.getError();
							} else {
								handleModelComponent(
										(ModelElement) elem,
										targetElement);
							}

						} else {
							error = Messages.CannotMoveToOtherDatabase;
						}

					} else if (elem instanceof CategoryElement) {
						// cast
						final CategoryElement cnElem = (CategoryElement) elem;
						// get database
						final IDatabase database = cnElem.getDatabase();

						// if databases match
						if (database.equals(targetDatabase)) {

							if (copyMode) {
								// copy
								final Copier copier = new Copier(
										(Category) cnElem.getData(), database,
										(Category) targetElement.getData());
								copier.copy();
								error = copier.getError();
							} else {
								// move
								handleCategory(
										(CategoryElement) elem,
										targetElement);
							}

						}
					} // end if
				}
			} catch (final Exception e) {
				log.error("Copy elements to target category element failed", e);
			}
		} // end if
		return error;
	}

	/**
	 * Handles the drop of a {@link CategoryElement}
	 * 
	 * @param element
	 *            The element to be dropped
	 * @param targetElement
	 *            The target element
	 * @throws DataProviderException
	 */
	private void handleCategory(final CategoryElement element,
			final CategoryElement targetElement)
			throws DataProviderException {
		final Category draggedCategory = (Category) element.getData();
		final Category targetCategory = (Category) targetElement.getData();
		// is target category a child of dragged category?? if so -> error
		final IDatabase database = element.getDatabase();

		if (database != null) {
			// update dragged category
			final Category parent = draggedCategory.getParentCategory();
			parent.remove(draggedCategory);
			database.refresh(parent);

			// update the target category
			targetCategory.add(draggedCategory);
			database.refresh(targetCategory);

			// update the parent of the dragged category
			draggedCategory.setParentCategory(targetCategory);
			database.refresh(draggedCategory);
		}
	}

	/**
	 * Handles the drop of a {@link ModelElement}
	 * 
	 * @param element
	 *            The element to be dropped
	 * @param targetElement
	 *            The target element
	 * @throws DataProviderException
	 */
	private void handleModelComponent(final ModelElement element,
			final CategoryElement targetElement)
			throws DataProviderException {
		final IDatabase database = element.getDatabase();
		final IDatabase targetDatabase = targetElement.getDatabase();
		if (database != null && targetDatabase != null) {
			IModelComponent modelComponent = (IModelComponent) element
					.getData();
			modelComponent = database.select(modelComponent.getClass(),
					modelComponent.getId());
			final Category category = (Category) targetElement.getData();
			modelComponent.setCategoryId(category.getId());
			if (database.equals(targetDatabase)) {
				database.refresh(modelComponent);
			} else {
				database.delete(modelComponent);
				targetDatabase.insert(modelComponent);
			}
		}
	}

	/**
	 * Caches the model component in the given model component navigation
	 * element
	 * 
	 * @param modelComponentElement
	 *            The element containing the model component to copy
	 */
	public void copy(final INavigationElement modelComponentElement) {
		cache = new INavigationElement[] { modelComponentElement };
		copyMode = true;
	}

	/**
	 * Caches the model components in the given model component navigation
	 * elements
	 * 
	 * @param modelComponentElements
	 *            The elements containing the model components to copy
	 */
	public void copy(final INavigationElement[] modelComponentElements) {
		cache = modelComponentElements;
		copyMode = true;
	}

	/**
	 * Caches the model component in the given model component navigation
	 * element
	 * 
	 * @param modelComponentElement
	 *            The element containing the model component to cut
	 */
	public void cut(final INavigationElement modelComponentElement) {
		cache = new INavigationElement[] { modelComponentElement };
		copyMode = false;
	}

	/**
	 * Caches the model components in the given model component navigation
	 * elements
	 * 
	 * @param modelComponentElements
	 *            The elements containing the model components to cut
	 */

	public void cut(final INavigationElement[] modelComponentElements) {
		cache = modelComponentElements;
		copyMode = false;
	}

	/**
	 * Looks up the cache
	 * 
	 * @return true if there are no elements in the cache, false otherwise
	 */
	public boolean isEmpty() {
		return cache == null || cache.length == 0;
	}

	/**
	 * Pastes the elements in the cache into the given category element
	 * 
	 * @param categoryElement
	 *            The category element to paste the copied/cutted element
	 */
	public void paste(final CategoryElement categoryElement) {
		if (!isEmpty()) {
			final String error = doIt(categoryElement);
			if (error == null) {
				Navigator.refresh();
				if (!copyMode) {
					cache = null;
				}
			} else {
				// open error message
				ErrorDialog.openError(UI.shell(),
						Messages.NavigationDropAssistant_ErrorTitle,
						Messages.NavigationDropAssistant_ErrorText, new Status(
								IStatus.ERROR, Activator.PLUGIN_ID, error));
			}
		}
	}
}
