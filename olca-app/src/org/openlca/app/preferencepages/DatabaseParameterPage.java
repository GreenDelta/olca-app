package org.openlca.app.preferencepages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseParameterPage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	private Action addParameterAction;
	private Action removeParameterAction;
	private IDatabase database;

	private List<Parameter> parameters = new ArrayList<>();

	private DatabaseParameterTable parameterTable;

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

		Section section = new Section(body, ExpandableComposite.NO_TITLE);
		UI.gridData(section, true, true);
		Composite composite = new Composite(section, SWT.NONE);
		UI.gridData(composite, true, true);
		UI.gridLayout(composite, 1);
		section.setClient(composite);

		parameterTable = new DatabaseParameterTable(composite);
		createActions(section);
		if (database != null) {
			loadParameters();
			addParameterAction.setEnabled(true);
			removeParameterAction.setEnabled(true);
			parameterTable.setEnabled(true);
		}
		return parent;
	}

	private void createActions(Section section) {
		addParameterAction = Actions.onAdd(() -> addParameter());
		removeParameterAction = Actions.onRemove(() -> deleteParameter());
		Actions.bind(section, addParameterAction, removeParameterAction);
		parameterTable.setActions(addParameterAction, removeParameterAction);
		removeParameterAction.setEnabled(false);
		addParameterAction.setEnabled(false);
	}

	private void loadParameters() {
		try {
			ParameterDao dao = new ParameterDao(database);
			List<Parameter> parameters = dao.getGlobalParameters();
			this.parameters.clear();
			for (Parameter parameter : parameters) {
				this.parameters.add(parameter);
			}
			parameterTable.setInput(this.parameters);
		} catch (final Exception e) {
			log.error("Loading database parameters failed", e);
		}
	}

	@Override
	public String getTitle() {
		return Messages.GlobalParameters;
	}

	@Override
	public boolean performOk() {
		if (database == null)
			return true;
		boolean b = Question.ask(Messages.SaveChanges,
				Messages.SaveChangesQuestion);
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

	private void addParameter() {
		Parameter parameter = new Parameter();
		parameter.setScope(ParameterScope.GLOBAL);
		String name = "p" + parameters.size();
		parameter.setName(name);
		parameters.add(parameter);
		parameter.setValue(1.0);
		parameter.setInputParameter(true);
		parameterTable.setInput(parameters);
	}

	private void deleteParameter() {
		Parameter parameter = parameterTable.getSelected();
		if (parameter == null)
			return;
		boolean b = Question
				.ask(Messages.DeleteParameter,
						Messages.DeleteDatabaseParameterQuestion);
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
			parameterTable.setInput(parameters);
		} catch (Exception e) {
			log.error("failed to delete parameter " + parameter, e);
		}
	}

}
