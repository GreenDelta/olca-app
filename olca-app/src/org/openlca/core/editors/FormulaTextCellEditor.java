package org.openlca.core.editors;

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.openlca.app.Messages;
import org.openlca.app.component.AutoCompleteTextCellEditor;
import org.openlca.core.model.Parameter;

/**
 * Text cell editor for editing formulas of a parameter with content assistance
 */
public class FormulaTextCellEditor extends AutoCompleteTextCellEditor {

	private List<Parameter> parameters;

	public FormulaTextCellEditor(TableViewer viewer, int column,
			List<Parameter> objectParameters) {
		super(viewer, column, Messages.SelectParameter);
		this.parameters = objectParameters;
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
			public String getText(final Object element) {
				final Parameter parameter = (Parameter) element;
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

}
