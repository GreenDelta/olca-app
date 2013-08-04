package org.openlca.app.viewers.table;

import java.util.Objects;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.viewers.table.modify.IModelChangedListener.ModelChangeType;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.Expression;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.openlca.core.model.Process;

public class ParameterViewer extends AbstractTableViewer<Parameter> {

	private interface LABEL {
		String NAME = Messages.Name;
		String FORMULA = Messages.Formula;
		String DESCRIPTION = Messages.Description;
	}

	private final static String[] COLUMN_HEADERS = { LABEL.NAME, LABEL.FORMULA,
			LABEL.DESCRIPTION };
	private Process process;

	public ParameterViewer(Composite parent) {
		super(parent);
		getCellModifySupport().support(LABEL.NAME, new NameModifier());
		getCellModifySupport().support(LABEL.FORMULA, new FormulaModifier());
		getCellModifySupport().support(LABEL.DESCRIPTION,
				new DescriptionModifier());
	}

	public void setInput(Process process) {
		this.process = process;
		if (process == null)
			setInput(new Parameter[0]);
		else
			setInput(process.getParameters().toArray(
					new Parameter[process.getParameters().size()]));
	}

	@Override
	protected String[] getColumnHeaders() {
		return COLUMN_HEADERS;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ParameterLabelProvider();
	}

	@OnCreate
	protected void onCreate() {
		Parameter parameter = new Parameter();
		parameter.setName("newParameter");
		parameter.setType(ParameterType.PROCESS);
		fireModelChanged(ModelChangeType.CREATE, parameter);
		setInput(process);
	}

	@OnRemove
	protected void onRemove() {
		for (Parameter parameter : getAllSelected())
			fireModelChanged(ModelChangeType.REMOVE, parameter);
		setInput(process);
	}

	private class ParameterLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Parameter))
				return null;
			Parameter parameter = (Parameter) element;
			switch (columnIndex) {
			case 0:
				return parameter.getName();
			case 1:
				if (parameter.getExpression().getFormula() != null)
					return parameter.getExpression().getFormula();
				else
					return Double
							.toString(parameter.getExpression().getValue());
			case 2:
				return parameter.getDescription();
			}
			return null;
		}
	}

	private class NameModifier extends TextCellModifier<Parameter> {

		@Override
		protected String getText(Parameter element) {
			return element.getName();
		}

		@Override
		protected void setText(Parameter element, String text) {
			if (!Objects.equals(text, element.getName())) {
				element.setName(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

	private class FormulaModifier extends TextCellModifier<Parameter> {

		@Override
		protected String getText(Parameter element) {
			if (element.getExpression().getFormula() == null)
				return Double.toString(element.getExpression().getValue());
			else
				return element.getExpression().getFormula();
		}

		@Override
		protected void setText(Parameter element, String text) {
			Expression expression = element.getExpression();
			try {
				// is number
				double value = Double.parseDouble(text);
				if (expression.getValue() != value
						|| expression.getFormula() != null) {
					expression.setFormula(null);
					expression.setValue(value);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			} catch (NumberFormatException e) {
				// is formula
				if (!Objects.equals(text, expression.getFormula())) {
					expression.setFormula(text);
					fireModelChanged(ModelChangeType.CHANGE, element);
				}
			}
		}

	}

	private class DescriptionModifier extends TextCellModifier<Parameter> {

		@Override
		protected String getText(Parameter element) {
			return element.getDescription();
		}

		@Override
		protected void setText(Parameter element, String text) {
			if (!Objects.equals(text, element.getDescription())) {
				element.setDescription(text);
				fireModelChanged(ModelChangeType.CHANGE, element);
			}
		}

	}

}
