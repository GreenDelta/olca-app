package org.openlca.app.editors.parameters;

import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.Error;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlobalParameterEditor extends ModelEditor<Parameter> {

	public static String ID = "editors.parameter";
	private Logger log = LoggerFactory.getLogger(getClass());
	private GlobalParameterInfoPage infoPage;

	public GlobalParameterEditor() {
		super(Parameter.class);
	}

	@Override
	protected void addPages() {
		try {
			addPage(infoPage = new GlobalParameterInfoPage(this));
			addCommentPage();
		} catch (Exception e) {
			log.error("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (infoPage.hasErrors()) {
			Error.showBox(M.CanNotSaveParameter);
			return;
		}
		String name = getModel().name;
		if (!Parameter.isValidName(name)) {
			Error.showBox(M.InvalidParameterName,
					name + " " + M.IsNotValidParameterName);
			return;
		}
		if (otherGlobalExists(name)) {
			Error.showBox(M.InvalidParameterName, M.ParameterWithSameNameExists);
			return;
		}
		super.doSave(monitor);
	}

	private boolean otherGlobalExists(String name) {
		ParameterDao dao = new ParameterDao(Database.get());
		String n = name.trim();
		for (Parameter global : dao.getGlobalParameters()) {
			if (Objects.equals(getModel(), global))
				continue;
			if (global.name == null)
				continue;
			if (n.equalsIgnoreCase(global.name.trim()))
				return true;
		}
		return false;
	}

}
