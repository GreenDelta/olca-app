package org.openlca.app.components;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.Messages;
import org.openlca.core.model.Parameter;

/**
 * Text cell editor for editing formulas with content assistance.
 */
public class FormulaTextCellEditor extends AutoCompleteTextCellEditor {

	private List<Parameter> parameters;

	public FormulaTextCellEditor(TableViewer viewer, int column,
			List<Parameter> parameters) {
		super(viewer, column, Messages.SelectTheParameterYouWantToReferTo);
		this.parameters = parameters;
	}

	public void updateParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	protected Parameter[] getInput() {
		return parameters.toArray(new Parameter[parameters.size()]);
	}

	@Override
	protected ILabelProvider getLabelProvider() {
		return new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (!(element instanceof Parameter))
					return null;
				Parameter parameter = (Parameter) element;
				String text = parameter.getName() + " = "
						+ parameter.getValue();
				// TODO: parameter unit if available
				return text;
			}
		};
	}

	@Override
	protected String getTextFromListElement(final Object element) {
		return ((Parameter) element).getName();
	}

}
