package org.openlca.app.editors.parameters.bigtable;

import java.util.Calendar;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.Version;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.util.Strings;

class ValueEditor {

	private final IDatabase db;
	private final EditorPage page;
	private final Param param;

	private ValueEditor(
		IDatabase db, EditorPage page, Param param) {
		this.db = db;
		this.page = page;
		this.param = param;
	}

	static boolean edit(EditorPage page, Param param) {
		var db = Database.get();
		if (db == null
			  || page == null
				|| param == null
				|| param.parameter == null)
			return false;
		return new ValueEditor(db, page, param).edit();
	}

	private boolean edit() {

		// first check that the parameter or the owner
		// is currently not edited in another editor
		if (hasOpenEditor())
			return false;

		var fi = page.buildInterpreter();
		var val = getDialogValue(fi);
		if (val == null)
			return false;

		var p = param.parameter;
		if (p.isInputParameter) {
			try {
				p.value = Double.parseDouble(val);
				param.evalError = false;
			} catch (Exception e) {
				param.evalError = true;
			}
		} else if (fi != null) {
			try {
				p.formula = val;
				var scope = param.isGlobal()
					? fi.getGlobalScope()
					: fi.getScopeOrGlobal(param.ownerId());
				p.value = scope.eval(val);
				param.evalError = false;
			} catch (Exception e) {
				param.evalError = true;
			}
		}

		// update the parameter and the owner
		// we also close a possible opened editor to make
		// sure that the user does not get confused with
		// a state that is not in sync with the database
		param.parameter = update(p);
		if (param.isGlobal()) {
			App.close(p);
		} else {
			var owner = getOwner();
			if (owner != null) {
				App.close(update(owner));
			}
		}
		return true;
	}

	private CategorizedEntity getOwner() {
		if (param.owner == null)
			return null;
		if (param.owner.type == ModelType.PROCESS)
			return db.get(Process.class, param.ownerId());
		if (param.owner.type == ModelType.IMPACT_CATEGORY)
			return db.get(ImpactCategory.class, param.ownerId());
		return null;
	}

	private <T extends CategorizedEntity> T update(T e) {
		if (e == null)
			return null;
		e.lastChange = Calendar.getInstance().getTimeInMillis();
		Version.incUpdate(e);
		return db.update(e);
	}

	private boolean hasOpenEditor() {
		var p = param.parameter;
		if (param.owner == null
				&& App.hasDirtyEditor(p)) {
			MsgBox.info("Cannot edit " + p.name,
				"The parameter is currently "
				+ "modified in another editor.");
			return true;
		}
		if (param.owner != null
				&& App.hasDirtyEditor(param.owner)) {
			var label = Strings.cut(Labels.name(param.owner), 50);
			MsgBox.info("Cannot edit " + p.name,
				label + " is currently modified in another editor.");
			return true;
		}
		return false;
	}

	private String getDialogValue(FormulaInterpreter interpreter) {
		var p = param.parameter;
		InputDialog dialog;
		if (p.isInputParameter) {
			dialog = new InputDialog(UI.shell(),
				"Edit value", "Set a new parameter value",
				Double.toString(p.value), s -> {
				try {
					Double.parseDouble(s);
					return null;
				} catch (Exception e) {
					return s + " " + M.IsNotValidNumber;
				}
			});
		} else {
			var scope = param.isGlobal()
				? interpreter.getGlobalScope()
				: interpreter.getScopeOrGlobal(param.ownerId());
			dialog = new InputDialog(UI.shell(),
				"Edit formula", "Set a new parameter formula",
				p.formula, s -> {
				try {
					scope.eval(s);
					return null;
				} catch (Exception e) {
					return s + " " + M.IsInvalidFormula;
				}
			});
		}

		return dialog.open() != Window.OK
			? null
			: dialog.getValue();
	}


}
