/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.app.preferencepages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.Messages;
import org.openlca.app.components.AutoCompleteTextCellEditor;
import org.openlca.core.model.Parameter;

/**
 * Text cell editor for editing formulas of a parameter with content assistance
 * 
 * @see AutoCompleteTextCellEditor
 */
public class FormulaTextCellEditor extends AutoCompleteTextCellEditor {

	/**
	 * The available parameters
	 */
	private Parameter[] input;

	/**
	 * Creates a new FormulaTextCellEditor
	 * 
	 * @param viewer
	 *            the owner of the text cell
	 * @param column
	 *            the column of the text cell
	 */
	public FormulaTextCellEditor(final TableViewer viewer, final int column) {
		super(viewer, column, Messages.SelectParameter);
	}

	@Override
	protected Parameter[] getInput() {
		final List<Parameter> parameters = new ArrayList<>();
		for (final Parameter parameter : input) {
			if (!parameter.getName().equals(
					((Parameter) getEditedElement()).getName())) {
				parameters.add(parameter);
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
	 * Setter of the input of the content assistant
	 * 
	 * @param input
	 *            The input of the content assistant
	 */
	public void setInput(final Parameter[] input) {
		this.input = input;
	}

}
