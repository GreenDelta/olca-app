/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.Messages;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Parameter;
import org.openlca.ui.autocomplete.AutoCompleteTextCellEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text cell editor for editing formulas of a parameter with content assistance
 * 
 * @see AutoCompleteTextCellEditor
 * 
 * @author Sebastian Greve
 * 
 */
public class FormulaTextCellEditor extends AutoCompleteTextCellEditor {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final IDatabase database;
	private Parameter[] objectParameters;

	/**
	 * Creates a new FormulaTextCellEditor
	 * 
	 * @param viewer
	 *            the owner of the text cell
	 * @param column
	 *            the column of the text cell
	 * @param objectParameters
	 *            the input for the content assistant
	 * @param database
	 *            The database
	 */
	public FormulaTextCellEditor(final TableViewer viewer, final int column,
			final Parameter[] objectParameters, final IDatabase database) {
		super(viewer, column, Messages.SelectParameter);
		this.objectParameters = objectParameters;
		this.database = database;
	}

	@Override
	protected Parameter[] getInput() {
		final List<String> parameterNames = new ArrayList<>();
		final List<Parameter> parameters = new ArrayList<>();
		final boolean overwriteLocal = Integer
				.parseInt(ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION
						.getValue(database.getUrl())) == ApplicationProperties.TOP_DOWN;

		// load database parameters
		List<Parameter> databaseParameters = null;
		try {
			final Map<String, Object> properties = new HashMap<>();
			properties.put("ownerId", "NULL");
			databaseParameters = database
					.selectAll(Parameter.class, properties);
		} catch (final Exception e) {
			log.error("Reading parameter from db failed", e);
		}

		// if sort direction is top down => add database parameters
		if (databaseParameters != null && overwriteLocal) {
			for (final Parameter p : databaseParameters) {
				parameters.add(p);
				parameterNames.add(p.getName());
			}
		}

		// add object's parameters
		for (final Parameter p : objectParameters) {
			if (!p.equals(getEditedElement())) {
				if (!overwriteLocal || !parameterNames.contains(p.getName())) {
					parameters.add(p);
				}
			}
			if (!overwriteLocal) {
				parameterNames.add(p.getName());
			}
		}

		// if sort direction is bottom up => add database parameters which are
		// not defined in the parameterizable object
		if (databaseParameters != null && !overwriteLocal) {
			for (final Parameter p : databaseParameters) {
				if (!parameterNames.contains(p.getName())) {
					parameters.add(p);
				}
			}
		}
		return parameters.toArray(new Parameter[parameters.size()]);
	}

	@Override
	protected ILabelProvider getLabelProvider() {
		return new LabelProvider() {

			@Override
			public String getText(final Object element) {
				final Parameter parameter = (Parameter) element;
				// name - value - description
				String text = parameter.getName() + " - "
						+ parameter.getExpression().getValue();
				if (parameter.getDescription() != null) {
					text += " - ";
					if (parameter.getDescription().length() > 75) {
						text += parameter.getDescription().substring(0, 75)
								+ "...";
					} else {
						text += parameter.getDescription();
					}
				}
				return text;
			}

		};
	}

	@Override
	protected String getTextFromListElement(final Object element) {
		return ((Parameter) element).getName();
	}

	/**
	 * Update the input for the content assistant
	 * 
	 * @param parameters
	 *            the new input
	 */
	public void updateParameters(final Parameter[] parameters) {
		objectParameters = parameters;
	}

}
