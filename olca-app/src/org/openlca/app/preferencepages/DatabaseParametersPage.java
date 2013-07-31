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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.ApplicationProperties;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.Question;
import org.openlca.app.util.Viewers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Expression;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseParametersPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private AddParameterAction addParameterAction;
	private IDatabase database;

	private final String FORMULA = Messages.Formula;
	private final String NAME = Messages.Name;
	private final String DESCRIPTION = Messages.Description;
	private final String NUMERIC_VALUE = Messages.NumericValue;
	private final String[] PROPERTIES = new String[] { NAME, FORMULA,
			NUMERIC_VALUE, DESCRIPTION };

	private List<Parameter> parameters = new ArrayList<>();

	private TableViewer parameterViewer;

	private RemoveParameterAction removeParameterAction;

	private int sortMode;

	@Override
	public void init(IWorkbench workbench) {
		log.trace("initialize database parameter page");
		database = Database.get();
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		getApplyButton().setVisible(false);
		getDefaultsButton().setVisible(false);
	}

	@Override
	protected Control createContents(final Composite parent) {
		log.trace("create content of database parameter page");
		Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		createDirectionRadios(body);

		Section section = new Section(body, ExpandableComposite.NO_TITLE);
		section.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
				true));
		Composite composite = new Composite(section, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));

		GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 0;

		composite.setLayout(layout);
		section.setClient(composite);

		parameterViewer = new TableViewer(composite, SWT.BORDER
				| SWT.FULL_SELECTION);
		parameterViewer.setContentProvider(new ArrayContentProvider());
		parameterViewer.setLabelProvider(new ParameterLabel());
		parameterViewer.getTable().setEnabled(false);
		Table table = parameterViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = parent.getParent()
				.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		table.setLayoutData(gd);
		for (String p : PROPERTIES) {
			TableColumn c = new TableColumn(table, SWT.NULL);
			c.setText(p);
		}
		for (TableColumn c : table.getColumns()) {
			if (c.getText().equals(NAME)) {
				c.setWidth(150);
			} else {
				c.pack();
			}
		}

		final ToolBarManager parametersBar = new ToolBarManager();

		// create add and remove actions
		addParameterAction = new AddParameterAction();
		removeParameterAction = new RemoveParameterAction();
		parametersBar.add(addParameterAction);
		parametersBar.add(removeParameterAction);
		final MenuManager parametersMenu = new MenuManager();
		section.setTextClient(parametersBar.createControl(section));
		parametersMenu.add(addParameterAction);
		parametersMenu.add(removeParameterAction);
		table.setMenu(parametersMenu.createContextMenu(table));
		removeParameterAction.setEnabled(false);
		addParameterAction.setEnabled(false);

		// create cell editors
		final CellEditor[] editors = new CellEditor[4];
		for (int i = 0; i < 4; i++) {
			if (i != 1) {
				editors[i] = new TextCellEditor(table);
			} else {
				editors[i] = new FormulaTextCellEditor(parameterViewer, i);
			}
		}

		parameterViewer.setColumnProperties(PROPERTIES);
		parameterViewer.setCellModifier(new ParameterModifier(parameterViewer,
				parameters));
		parameterViewer.setCellEditors(editors);

		if (database != null) {
			loadParameters();
			sortMode = Integer
					.parseInt(ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION
							.getValue(database.toString()));

			addParameterAction.setEnabled(true);
			removeParameterAction.setEnabled(true);
			parameterViewer.getTable().setEnabled(true);
			updateCellEditors();
		}

		return parent;
	}

	private void createDirectionRadios(Composite body) {
		Composite radioComposite = new Composite(body, SWT.NONE);
		radioComposite.setLayout(new GridLayout(2, true));
		Label nameLabel = new Label(radioComposite, SWT.NONE);
		nameLabel.setText(Messages.Direction);
		new Label(radioComposite, SWT.NONE);

		Button topDownButton = new Button(radioComposite, SWT.RADIO);
		topDownButton.setText(Messages.ParametersPreferencePage_TopDown);
		topDownButton.setSelection(false);
		topDownButton.setEnabled(false);
		topDownButton.addSelectionListener(new EvaluationDirectionChange(
				ApplicationProperties.TOP_DOWN));

		Button bottomUpButton = new Button(radioComposite, SWT.RADIO);
		bottomUpButton.setText(Messages.ParametersPreferencePage_BottomUp);
		bottomUpButton.setSelection(false);
		bottomUpButton.setEnabled(false);
		bottomUpButton.addSelectionListener(new EvaluationDirectionChange(
				ApplicationProperties.BOTTOM_UP));

		if (database != null) {
			topDownButton.setEnabled(true);
			topDownButton
					.setSelection(sortMode == ApplicationProperties.TOP_DOWN);
			bottomUpButton.setEnabled(true);
			bottomUpButton
					.setSelection(sortMode == ApplicationProperties.BOTTOM_UP);
		}
	}

	private void loadParameters() {
		try {
			ParameterDao dao = new ParameterDao(database);
			List<Parameter> parameters = dao
					.getAllForType(ParameterType.DATABASE);
			this.parameters.clear();
			for (Parameter parameter : parameters) {
				this.parameters.add(parameter);
			}
			parameterViewer.setInput(this.parameters);
		} catch (final Exception e) {
			log.error("Loading database parameters failed", e);
		}
	}

	private void updateCellEditors() {
		for (CellEditor editor : parameterViewer.getCellEditors()) {
			if (editor instanceof FormulaTextCellEditor) {
				((FormulaTextCellEditor) editor).setInput(parameters
						.toArray(new Parameter[parameters.size()]));
			}
		}
	}

	@Override
	public String getTitle() {
		return Messages.GlobalParametersPreferencePage_Title;
	}

	@Override
	public boolean performOk() {
		if (database == null)
			return true;
		boolean b = Question.ask("Save changes?",
				"Do you want to save the changes?");
		if (!b)
			return true;
		try {
			ParameterDao dao = new ParameterDao(database);
			for (Parameter p : parameters) {
				if (p.getId() > 0L)
					dao.update(p);
				else
					dao.insert(p);
			}
			return true;
		} catch (Exception e) {
			log.error("failed to save database parameters", e);
			return false;
		}

	}

	private class EvaluationDirectionChange implements SelectionListener {

		private int mode;

		public EvaluationDirectionChange(int mode) {
			this.mode = mode;
		}

		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			widgetSelected(e);
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			log.trace("direction of parameter evaluation changed mode = {}",
					mode);
			sortMode = mode;
		}

	}

	private class AddParameterAction extends Action {

		public AddParameterAction() {
			setText(NLS.bind(Messages.AddAction_Text, Messages.Parameter));
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			Parameter parameter = new Parameter(new Expression("1", 1),
					ParameterType.DATABASE, null);
			String name = "p" + parameters.size();
			parameter.setName(name);
			parameters.add(parameter);
			parameterViewer.setInput(parameters);
			parameterViewer.setSelection(new StructuredSelection(parameter));
			updateCellEditors();
		}
	}

	private class RemoveParameterAction extends Action {

		public RemoveParameterAction() {
			setText(NLS.bind(Messages.RemoveAction_Text, Messages.Parameter));
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());
		}

		@Override
		public void run() {
			Parameter parameter = Viewers.getFirstSelected(parameterViewer);
			if (parameter == null)
				return;
			boolean b = Question.ask(
					"Delete paramater",
					"Do you really want to delete parameter "
							+ parameter.getName() + " from the database?");
			if (!b)
				return;
			tryDelete(parameter);
		}

		private void tryDelete(Parameter parameter) {
			try {
				if (parameter.getId() > 0L) {
					ParameterDao dao = new ParameterDao(database);
					dao.delete(parameter);
				}
				parameters.remove(parameter);
				parameterViewer.setInput(parameters);
				updateCellEditors();
			} catch (Exception e) {
				log.error("failed to delete parameter " + parameter, e);
			}
		}
	}

}
