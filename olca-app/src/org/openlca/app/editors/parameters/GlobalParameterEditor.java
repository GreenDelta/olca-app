package org.openlca.app.editors.parameters;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.usage.ParameterUsageTree;
import org.openlca.core.model.Parameter;
import org.openlca.util.Parameters;
import org.openlca.util.Strings;

public class GlobalParameterEditor extends ModelEditor<Parameter> {

	public static String ID = "editors.parameter";
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
			ErrorReporter.on("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (infoPage.hasErrors()) {
			MsgBox.error(M.CanNotSaveParameter);
			return;
		}

		// check the new parameter name
		var newName = getModel().name;
		if (!Parameters.isValidName(newName)) {
			MsgBox.error(M.InvalidParameterName,
					newName + " " + M.IsNotValidParameterName);
			return;
		}
		if (otherGlobalExists(newName)) {
			MsgBox.error(M.InvalidParameterName,
					M.ParameterWithSameNameExists);
			return;
		}

		// check if we need to trigger the
		// rename dialog
		var oldName = getOldName();
		if (oldName.isEmpty()
				|| eq(oldName.get(), newName)
				|| isUnused(monitor)) {
			super.doSave(monitor);
			return;
		}

		// reset the name, save it and open
		// the rename dialog
		getModel().name = oldName.get();
		super.doSave(monitor);
		var param = new ParameterDao(Database.get())
				.getForId(getModel().id);
		App.close(param);
		RenameParameterDialog.open(param, newName);
	}

	private boolean otherGlobalExists(String newName) {
		var dao = new ParameterDao(Database.get());
		var name = newName.trim();
		for (var global : dao.getGlobalParameters()) {
			if (Objects.equals(getModel(), global))
				continue;
			if (global.name == null)
				continue;
			if (name.equalsIgnoreCase(global.name.trim()))
				return true;
		}
		return false;
	}

	private Optional<String> getOldName() {
		var param = getModel();
		if (param.id == 0)
			return Optional.empty();
		var old = new ParameterDao(Database.get()).getForId(param.id);
		return old == null || Strings.nullOrEmpty(old.name)
				? Optional.empty()
				: Optional.of(old.name);
	}

	private boolean isUnused(IProgressMonitor monitor) {
		monitor.beginTask(
				"Search for parameter usage",
				IProgressMonitor.UNKNOWN);
		var db = Database.get();
		var param = getModel();
		var old = new ParameterDao(db).getForId(param.id);
		var tree = ParameterUsageTree.of(old, db);
		return tree.isEmpty();
	}

	private boolean eq(String a, String b) {
		if (a == null || b == null)
			return false;
		var _a = a.trim().toLowerCase();
		var _b = b.trim().toLowerCase();
		return _a.equals(_b);
	}
}
