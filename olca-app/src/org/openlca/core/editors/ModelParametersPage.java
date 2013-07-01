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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.DeleteWithQuestionAction;
import org.openlca.core.application.evaluation.EvaluationListener;
import org.openlca.core.math.FormulaParseException;
import org.openlca.core.model.Expression;
import org.openlca.core.model.IParameterisable;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.UIFactory;

/**
 * Abstract form page for editing parameters of a parameterizable object
 * 
 * @author Sebastian Greve
 */
public class ModelParametersPage extends ModelEditorPage implements
		EvaluationListener, PropertyChangeListener {

	private final IParameterisable component;
	private final String formText;
	private FormulaTextCellEditor formulaEditor;
	private IMessageManager messageManager;

	private String PARAMETER_DESCRIPTION = Messages.Common_Description;
	private String PARAMETER_FORMULA = Messages.Common_FormulaTitle;
	private String PARAMETER_NAME = Messages.Common_Name;
	private String PARAMETER_NUMERIC_VALUE = Messages.NumericValue;
	private String[] PARAMETER_PROPERTIES = new String[] { PARAMETER_NAME,
			PARAMETER_FORMULA, PARAMETER_NUMERIC_VALUE, PARAMETER_DESCRIPTION };

	private TableViewer parameterViewer;

	public ModelParametersPage(ModelEditor editor, String formText) {
		super(editor, "ParameterInfoPage", Messages.ParametersPageLabel); //$NON-NLS-1$
		component = (IParameterisable) editor.getModelComponent();
		this.formText = formText;
	}

	private Parameter parameterExists(String name) {
		if (name == null)
			return null;
		for (Parameter p : component.getParameters()) {
			if (name.trim().equalsIgnoreCase(p.getName()))
				return p;
		}
		return null;
	}

	@Override
	protected void createContents(final Composite body,
			final FormToolkit toolkit) {
		messageManager = getForm().getMessageManager();

		Section section = UI.section(body, toolkit,
				Messages.ParametersPageLabel);
		UI.gridData(section, true, true);
		Composite composite = UI.sectionClient(section, toolkit);
		UI.gridLayout(composite, 1);
		createViewer(toolkit, composite);
		createCellEditors();
		bindActions(parameterViewer, section);
	}

	private void createViewer(final FormToolkit toolkit, Composite composite) {
		parameterViewer = UIFactory.createTableViewer(composite, null, null,
				toolkit, PARAMETER_PROPERTIES);
		parameterViewer.setCellModifier(new ParameterCellModifier());
		parameterViewer.setLabelProvider(new ParameterLabelProvider());
		UI.gridData(parameterViewer.getTable(), true, true);
		parameterViewer.getTable().getColumn(0).setWidth(100);
		parameterViewer.getTable().getColumn(1).setWidth(100);
	}

	private void createCellEditors() {
		CellEditor[] parametersEditors = new CellEditor[4];
		for (int i = 0; i < 4; i++) {
			if (i != 1) {
				parametersEditors[i] = new TextCellEditor(
						parameterViewer.getTable());
			} else {
				formulaEditor = new FormulaTextCellEditor(parameterViewer, 1,
						component.getParameters());
				parametersEditors[i] = formulaEditor;
			}
		}
		parameterViewer.setCellEditors(parametersEditors);
	}

	private void bindActions(TableViewer paramViewer, Section section) {
		Action addAction = new AddParameterAction();
		Action removeAction = new RemoveParameterAction();
		UI.bindActions(section, addAction, removeAction);
		UI.bindActions(paramViewer, addAction, removeAction);
	}

	@Override
	protected String getFormTitle() {
		final String title = formText
				+ ": "
				+ (component != null ? ((IModelComponent) component).getName() != null ? ((IModelComponent) component)
						.getName() : ""
						: "");
		return title;
	}

	@Override
	protected void setData() {
		parameterViewer.setInput(component.getParameters());
	}

	@Override
	public void dispose() {
		if (getEditor() != null
				&& ((ModelEditor) getEditor()).getModelComponent() != null) {
			((ModelEditor) getEditor()).getModelComponent()
					.removePropertyChangeListener(this);
		}
		super.dispose();
	}

	@Override
	public void error(FormulaParseException exception) {
		if (messageManager == null)
			return;
		boolean belongsToThePage = false;
		int i = 0;
		while (!belongsToThePage && i < component.getParameters().length) {
			if (component.getParameters()[i].getId().equals(
					exception.getTriggerId())) {
				belongsToThePage = true;
			} else {
				i++;
			}
		}
		if (belongsToThePage) {
			messageManager.addMessage(exception.toString(),
					exception.getMessage(), parameterViewer,
					IMessageProvider.ERROR);
		}
	}

	@Override
	public void evaluated() {
		if (parameterViewer != null) {
			messageManager.removeAllMessages();
			parameterViewer.refresh();
		}
	}

	@Override
	public void propertyChange(final PropertyChangeEvent arg0) {
		if (parameterViewer != null) {
			if (arg0.getPropertyName().equals("value")) { //$NON-NLS-1$
				messageManager.removeAllMessages();
				parameterViewer.refresh();
			}
		}
	}

	/**
	 * Adds a parameter object to this process
	 * 
	 * @see Action
	 */
	private class AddParameterAction extends Action {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.editors.ModelParametersPage.AddParameterAction"; //$NON-NLS-1$

		/**
		 * Creates a new AddParameterAction and sets the ID, TEXT and
		 * ImageDescriptor
		 */
		public AddParameterAction() {
			setId(ID);
			setText(Messages.Common_NewParameter);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			// create parameter
			final Parameter parameter = new Parameter(UUID.randomUUID()
					.toString(),
					new Expression("1", 1), ParameterType.getTypeFor(component //$NON-NLS-1$
							.getClass()), ((IModelComponent) component).getId());
			String name = "p" + component.getParameters().length; //$NON-NLS-1$
			int i = 1;
			while (parameterExists(name) != null) {
				name = "p" + (component.getParameters().length + i); //$NON-NLS-1$
				i++;
			}
			parameter.setName(name);
			component.add(parameter);

			// update table viewer and formula editor
			parameterViewer.setInput(component.getParameters());
			parameterViewer.setSelection(new StructuredSelection(parameter));
			formulaEditor.updateParameters(component.getParameters());
		}
	}

	/**
	 * A cell modifier for the parametersTableViewer
	 * 
	 * @see ICellModifier
	 */
	private class ParameterCellModifier implements ICellModifier {

		@Override
		public boolean canModify(final Object element, final String property) {
			if (!property.equals(PARAMETER_NUMERIC_VALUE)) {
				return true;
			}
			return false;
		}

		@Override
		public Object getValue(final Object element, final String property) {
			Object v = null;
			if (element instanceof Parameter) {
				final Parameter parameter = (Parameter) element;
				if (property.equals(PARAMETER_NAME)) {
					v = parameter.getName();
				} else if (property.equals(PARAMETER_FORMULA)) {
					v = parameter.getExpression().getFormula();
				} else if (property.equals(PARAMETER_DESCRIPTION)) {
					v = parameter.getDescription();
				}
			}
			return v != null ? v : ""; //$NON-NLS-1$
		}

		@Override
		public void modify(Object element, final String property,
				final Object value) {
			if (element instanceof Item) {
				element = ((Item) element).getData();
			}
			if (element instanceof Parameter) {
				final Parameter parameter = (Parameter) element;

				if (property.equals(PARAMETER_NAME)) {
					// check if parameter exists
					final boolean exists = parameterExists(value.toString()) != null
							&& !parameterExists(value.toString()).equals(
									parameter);
					if (!exists) {
						// check parameter name
						if (Parameter.checkName(value.toString())) {
							// set name
							parameter.setName(value.toString());
						}
					} else {
						// open error message
						MessageDialog.openError(UI.shell(),
								Messages.ModelParametersPageDuplicateParameter,
								Messages.Common_ParameterAlreadyDefined);
					}
				} else if (property.equals(PARAMETER_FORMULA)) {
					// set formula
					parameter.getExpression().setFormula(value.toString());
				} else if (property.equals(PARAMETER_DESCRIPTION)) {
					// set description
					parameter.setDescription(value.toString());
				}
			}
			// refresh parameter viewer
			parameterViewer.setInput(component.getParameters());
			parameterViewer.refresh();
		}
	}

	/**
	 * Removes the selected parameter object from this process
	 * 
	 * @see Action
	 */
	private class RemoveParameterAction extends DeleteWithQuestionAction {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.editors.ModelParametersPage.RemoveParameterAction"; //$NON-NLS-1$

		/**
		 * The text of the action
		 */
		public String TEXT = Messages.RemoveParameterText;

		/**
		 * Creates a new RemoveParameterAction and sets the ID, TEXT and
		 * ImageDescriptor
		 */
		public RemoveParameterAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void delete() {
			final StructuredSelection structuredSelection = (StructuredSelection) parameterViewer
					.getSelection();
			// for each selected parameter
			for (int i = 0; i < structuredSelection.toArray().length; i++) {
				final Parameter parameter = (Parameter) structuredSelection
						.toArray()[i];
				// remove from component
				component.remove(parameter);
			}
			// update table viewer and formula editor
			parameterViewer.setInput(component.getParameters());
			formulaEditor.updateParameters(component.getParameters());
		}

	}
}
