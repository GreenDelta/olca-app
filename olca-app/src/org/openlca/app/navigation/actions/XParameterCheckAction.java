package org.openlca.app.navigation.actions;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Question;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.NativeSql.QueryResultHandler;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.matrix.ParameterTable;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.expressions.FormulaInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XParameterCheckAction extends Action implements INavigationAction {

	private Logger log = LoggerFactory.getLogger(getClass());

	public XParameterCheckAction() {
		setImageDescriptor(ImageType.EXTENSION_ICON.getDescriptor());
		setText("Check Parameters");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		boolean doIt = Question.ask("Check formulas?",
				"Do you want to check all the formulas in the database?");
		if (!doIt)
			return;
		App.run("Check parameters", new Runnable() {
			public void run() {
				new Runner().doIt();
			}
		});
	}

	private class Runner {

		private HashMap<Long, ProcessDescriptor> processes;
		private FormulaInterpreter interpreter;

		void doIt() {
			log.trace("begin formula check");
			IDatabase database = Database.get();
			init(database);
			evalAll(database);
		}

		private void init(IDatabase database) {
			log.trace("init formula interpreter");
			processes = new HashMap<>();
			ProcessDao dao = new ProcessDao(database);
			for (ProcessDescriptor descriptor : dao.getDescriptors())
				processes.put(descriptor.getId(), descriptor);
			ParameterTable parameterTable = ParameterTable.build(database,
					processes.keySet());
			interpreter = parameterTable.createInterpreter();
		}

		private void evalAll(IDatabase database) {
			String query = "select name, f_owner, formula from tbl_parameters "
					+ "where scope = 'PROCESS'";
			try {
				NativeSql.on(database).query(query, new QueryResultHandler() {
					@Override
					public boolean nextResult(ResultSet result)
							throws SQLException {
						evalRecord(result);
						return true;
					}
				});
			} catch (Exception e) {
				log.error("failed to query parameter table", e);
			}
		}

		private void evalRecord(ResultSet result) throws SQLException {
			String var = result.getString("name");
			long process = result.getLong("f_owner");
			ProcessDescriptor descriptor = processes.get(process);
			try {
				interpreter.getScope(process).eval(var);
				log.trace("evaluated {} in {} sucessfully", var, descriptor);
			} catch (Throwable e) {
				log.error("failed to evaluate {} in {}", var, descriptor);
			}
		}
	}
}
