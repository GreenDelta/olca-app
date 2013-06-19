/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.application.preferencepages;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.core.application.ApplicationProperties;
import org.openlca.core.application.Messages;
import org.openlca.core.application.evaluation.EvaluationController;
import org.openlca.core.application.evaluation.EvaluationListener;
import org.openlca.core.application.evaluation.ParametrizableComponentUpdater;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.math.FormulaParseException;
import org.openlca.core.model.Expression;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterType;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.BaseNameSorter;
import org.openlca.ui.Error;
import org.openlca.ui.Question;
import org.openlca.ui.UI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseParametersPreferencePage extends
		AbstractDatabasePreferencePage implements PropertyChangeListener,
		EvaluationListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private AddParameterAction addParameterAction;
	private Button bottomUpButton;
	private Button topDownButton;
	private IDatabase database;
	private EvaluationController evaluationController;
	private final List<FormulaParseException> exceptions = new ArrayList<>();

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

	private final PropertyChangeSupport support = new PropertyChangeSupport(
			this);

	public DatabaseParametersPreferencePage() {
		super();
	}

	public DatabaseParametersPreferencePage(String title) {
		super(title);
	}

	public DatabaseParametersPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	private void initEvaluationController() {
		try {
			evaluationController = new EvaluationController(database);
			for (Parameter parameter : parameters) {
				evaluationController.registerParameter(parameter);
			}
		} catch (Exception e) {
			log.error("Initializing evaluation controller failed", e);
		}
		evaluationController.addEvaluationListener(this);
	}

	/**
	 * Initializes the listeners
	 */
	private void initListeners() {

		topDownButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				sortMode = ApplicationProperties.TOP_DOWN;
				getApplyButton().setEnabled(true);
				setDirty(true);
			}
		});

		bottomUpButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				sortMode = ApplicationProperties.BOTTOM_UP;
				getApplyButton().setEnabled(true);
				setDirty(true);
			}

		});

		parameterViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(
							final SelectionChangedEvent event) {
						if (event.getSelection().isEmpty()) {
							removeParameterAction.setEnabled(false);
						} else {
							removeParameterAction.setEnabled(true);
						}
					}
				});
	}

	private void loadParameters() {
		try {
			ParameterDao dao = new ParameterDao(database.getEntityFactory());
			List<Parameter> parameters = dao
					.getAllForType(ParameterType.DATABASE);
			this.parameters.clear();
			for (Parameter parameter : parameters) {
				parameter
						.addPropertyChangeListener(DatabaseParametersPreferencePage.this);
				this.parameters.add(parameter);
			}
			parameterViewer.setInput(this.parameters
					.toArray(new Parameter[this.parameters.size()]));
			initEvaluationController();
		} catch (final Exception e) {
			log.error("Loading database parameters failed", e);
		}
	}

	/**
	 * Opens a dialog on who to react to an error. Possible answers are
	 * "Restore" and "Cancel"
	 * 
	 * @return true if the answer was Restore, false otherwise
	 */
	private boolean openErrorQuestion() {
		boolean restored = false;
		// open dialog
		final MessageDialog dialog = new MessageDialog(UI.shell(),
				Messages.DatabaseParametersPreferencePage_ErrorsOccured, null,
				Messages.DatabaseParametersPreferencePage_ErrorText,
				MessageDialog.QUESTION, new String[] {
						Messages.DatabaseParametersPreferencePage_Restore,
						Messages.DatabaseParametersPreferencePage_Cancel }, 1);
		if (dialog.open() == 0) {
			// "restore" was selected
			restored = true;
		}
		return restored;
	}

	/**
	 * Saves the changes to the database
	 * 
	 * @param apply
	 *            Indicates if the apply button was pressed
	 * @return True if the changes were saved or the previous state was
	 *         restored, false otherwise
	 */
	private boolean save(final boolean apply) {
		boolean saved = false;
		final ParametrizableComponentUpdater updater = new ParametrizableComponentUpdater();
		if (!updater.update(parameters, database, sortMode)) {
			try {
				ParameterDao dao = new ParameterDao(database.getEntityFactory());
				List<Parameter> dbParameters = dao
						.getAllForType(ParameterType.DATABASE);
				List<Parameter> temp = new ArrayList<>();
				for (Parameter parameter : parameters) {
					temp.add(parameter);
				}

				for (Parameter p : dbParameters) {
					if (temp.contains(p)) {
						database.refresh(temp.get(temp.indexOf(p)));
						temp.remove(p);
					} else {
						database.delete(p);
					}
				}
				for (Parameter parameter : temp) {
					database.insert(parameter);
				}
			} catch (final Exception e) {
				log.error("Save to database failed", e);
			}
			getApplyButton().setEnabled(false);
			setDirty(false);
			saved = true;
			ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION.setValue(
					sortMode + "", database.getUrl());
		}
		if (!saved) {
			final boolean restore = openErrorQuestion();
			if (apply && restore) {
				// restore previous state
				loadParameters();
				sortMode = Integer
						.parseInt(ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION
								.getValue(database.getUrl()));
				topDownButton
						.setSelection(sortMode == ApplicationProperties.TOP_DOWN);
				bottomUpButton
						.setSelection(sortMode == ApplicationProperties.BOTTOM_UP);
				getApplyButton().setEnabled(false);
				setDirty(false);
				exceptions.clear();
			}
		}
		return saved;
	}

	/**
	 * Updates the text cell editors
	 */
	private void updateCellEditors() {
		for (CellEditor editor : parameterViewer.getCellEditors()) {
			if (editor instanceof FormulaTextCellEditor) {
				((FormulaTextCellEditor) editor).setInput(parameters
						.toArray(new Parameter[parameters.size()]));
			}
		}
	}

	@Override
	protected Control createContents(final Composite parent) {
		// create body
		final Composite body = new Composite(parent, SWT.NONE);
		body.setLayout(new GridLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		super.createContents(body);

		// radio "group" composite
		final Composite radioComposite = new Composite(body, SWT.NONE);
		radioComposite.setLayout(new GridLayout(2, true));

		final Label nameLabel = new Label(radioComposite, SWT.NONE);
		nameLabel.setText(Messages.Direction);
		new Label(radioComposite, SWT.NONE);

		// button for selecting top down sort direction
		topDownButton = new Button(radioComposite, SWT.RADIO);
		topDownButton.setText(Messages.ParametersPreferencePage_TopDown);
		topDownButton.setSelection(false);
		topDownButton.setEnabled(false);

		// button for selecting bottom up sort direction
		bottomUpButton = new Button(radioComposite, SWT.RADIO);
		bottomUpButton.setText(Messages.ParametersPreferencePage_BottomUp);
		bottomUpButton.setSelection(false);
		bottomUpButton.setEnabled(false);

		// section for parameter table viewer
		final Section section = new Section(body, ExpandableComposite.NO_TITLE);
		section.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true,
				true));

		final Composite composite = new Composite(section, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL,
				true, true));

		final GridLayout layout = new GridLayout();
		layout.marginWidth = 10;
		layout.marginHeight = 0;

		composite.setLayout(layout);
		section.setClient(composite);

		// table viewer to display and edit the parameters of the selected
		// database
		parameterViewer = new TableViewer(composite, SWT.BORDER
				| SWT.FULL_SELECTION);
		parameterViewer.setContentProvider(new ArrayContentProvider());
		parameterViewer.setSorter(new BaseNameSorter());
		parameterViewer.setLabelProvider(new ParameterLabel());
		parameterViewer.getTable().setEnabled(false);

		final Table table = parameterViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		final GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = parent.getParent()
				.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		table.setLayoutData(gd);

		for (final String p : PROPERTIES) {
			final TableColumn c = new TableColumn(table, SWT.NULL);
			c.setText(p);
		}

		for (final TableColumn c : table.getColumns()) {
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

		support.addPropertyChangeListener(removeParameterAction);

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
		parameterViewer.setCellModifier(new ParameterModifier(parameterViewer));
		parameterViewer.setCellEditors(editors);

		initListeners();
		return parent;
	}

	@Override
	protected void onDatabaseSelection(final IDatabase selectedDatabase) {
		database = selectedDatabase;
		loadParameters();
		sortMode = Integer
				.parseInt(ApplicationProperties.PROP_PARAMETER_SORT_DIRECTION
						.getValue(database.getUrl()));

		topDownButton.setEnabled(true);
		topDownButton.setSelection(sortMode == ApplicationProperties.TOP_DOWN);
		bottomUpButton.setEnabled(true);
		bottomUpButton
				.setSelection(sortMode == ApplicationProperties.BOTTOM_UP);
		addParameterAction.setEnabled(true);

		parameterViewer.getTable().setEnabled(true);
		updateCellEditors();
	}

	@Override
	protected void performApply() {
		save(true);
	}

	@Override
	protected void save() {
		save(true);
	}

	@Override
	public void createControl(final Composite parent) {
		super.createControl(parent);
		getApplyButton().setEnabled(false);
		getDefaultsButton().setVisible(false);
	}

	@Override
	public void dispose() {
		if (evaluationController != null) {
			evaluationController.removeEvaluationListener(this);
			evaluationController = null;
		}
		super.dispose();
	}

	@Override
	public void error(FormulaParseException exception) {
		Error.showPopup("Parameter evaluation failed", exception.getMessage());
		exceptions.add(exception);
		getApplyButton().setEnabled(false);
	}

	@Override
	public void evaluated() {
		exceptions.clear();
		getApplyButton().setEnabled(isDirty());
	}

	@Override
	public String getTitle() {
		return Messages.GlobalParametersPreferencePage_Title;
	}

	@Override
	public void init(final IWorkbench workbench) {
		// nothing to initialize
	}

	@Override
	public boolean performOk() {
		boolean saved = true;
		if (isDirty()) {
			if (exceptions.size() == 0) {
				if (Question.ask(Messages.Save_Resource,
						Messages.Common_SaveChangesQuestion)) {
					saved = save(false);
				}
			} else {
				saved = openErrorQuestion();
			}
		}
		return saved;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (!evt.getPropertyName().equals("value")) {
			setDirty(true);
			getApplyButton().setEnabled(true);
		}
	}

	/**
	 * Action for adding a new parameter
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class AddParameterAction extends Action {

		public static final String ID = "org.openlca.core.appplication.preferencepages.GlobalParametersPreferencePage.AddParameterAction";
		public String TEXT = NLS.bind(Messages.AddAction_Text,
				Messages.Parameter);

		public AddParameterAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.ADD_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.ADD_ICON_DISABLED
					.getDescriptor());
		}

		private Parameter parameterExists(final String name) {
			Parameter exists = null;
			int i = 0;
			while (exists == null && i < parameters.size()) {
				if (parameters.get(i).getName().equals(name)) {
					exists = parameters.get(i);
				} else {
					i++;
				}
			}
			return exists;
		}

		@Override
		public void run() {
			// create new parameter
			final Parameter parameter = new Parameter(UUID.randomUUID()
					.toString(), new Expression("1", 1),
					ParameterType.DATABASE, null);
			String name = "p" + parameters.size();
			int i = parameters.size() + 1;
			// get unique name
			while (parameterExists(name) != null) {
				name = "p" + i;
				i++;
			}
			parameter.setName(name);
			parameters.add(parameter);
			parameter
					.addPropertyChangeListener(DatabaseParametersPreferencePage.this);

			// register parameter
			evaluationController.registerParameter(parameter);

			// set parameters table viewer input
			parameterViewer.setInput(parameters
					.toArray(new Parameter[parameters.size()]));
			setDirty(true);
			getApplyButton().setEnabled(true);
			parameterViewer.setSelection(new StructuredSelection(parameter));
			support.firePropertyChange("newParameter", null, parameter);

			// evaluate
			evaluationController.evaluate();

			// update cell editors
			updateCellEditors();
		}
	}

	/**
	 * Action for removing a parameter
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class RemoveParameterAction extends Action implements
			PropertyChangeListener {

		/**
		 * The id of the action
		 */
		public static final String ID = "org.openlca.editors.ProcessInfoPage.RemoveParameterAction";

		/**
		 * The text of the action
		 */
		public String TEXT = NLS.bind(Messages.RemoveAction_Text,
				Messages.Parameter);

		/**
		 * Creates a new instance
		 */
		public RemoveParameterAction() {
			setId(ID);
			setText(TEXT);
			setImageDescriptor(ImageType.DELETE_ICON.getDescriptor());
			setDisabledImageDescriptor(ImageType.DELETE_ICON_DISABLED
					.getDescriptor());

		}

		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			setEnabled(parameters.size() > 0);
		}

		@Override
		public void run() {
			// get first selected parameter
			final StructuredSelection structuredSelection = (StructuredSelection) parameterViewer
					.getSelection();
			final Parameter parameter = (Parameter) structuredSelection
					.getFirstElement();

			// unregister the parameter
			evaluationController.unregisterParameter(parameter);
			parameters.remove(parameter);
			parameter
					.removePropertyChangeListener(DatabaseParametersPreferencePage.this);

			// reevaluate
			evaluationController.evaluate();

			// set parameter table viewer input
			parameterViewer.setInput(parameters
					.toArray(new Parameter[parameters.size()]));
			setDirty(true);
			getApplyButton().setEnabled(true);

			// update cell editors
			updateCellEditors();
		}

	}

}
