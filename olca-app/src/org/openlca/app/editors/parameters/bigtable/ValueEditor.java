package org.openlca.app.editors.parameters.bigtable;

import java.util.Calendar;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.UncertaintyDialog;
import org.openlca.app.db.Database;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.RootEntity;
import org.openlca.core.model.Uncertainty;
import org.openlca.core.model.UncertaintyType;
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

		// open the dialog and set new parameter values
		var interpreter = page.buildInterpreter();
		var dialog = new Dialog(param, interpreter);
		if (dialog.open() != Window.OK)
			return false;

		var p = param.parameter;
		if (p.isInputParameter) {
			p.value = dialog.value;
			var u = dialog.uncertainty;
			if (u != null) {
				p.uncertainty = u.distributionType == UncertaintyType.NONE
					? null
					: u;
			}
		} else {
			p.formula = dialog.formula == null
				? ""
				: dialog.formula;
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

	private RootEntity getOwner() {
		if (param.owner == null)
			return null;
		if (param.owner.type == ModelType.PROCESS)
			return db.get(Process.class, param.ownerId());
		if (param.owner.type == ModelType.IMPACT_CATEGORY)
			return db.get(ImpactCategory.class, param.ownerId());
		return null;
	}

	private <T extends RootEntity> T update(T e) {
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

	private static class Dialog extends FormDialog {

		private final Param param;
		private final FormulaInterpreter interpreter;

		private double value;
		private String formula;
		private Uncertainty uncertainty;

		Dialog(Param param, FormulaInterpreter interpreter) {
			super(UI.shell());
			this.param = param;
			this.interpreter = interpreter;
			var p = param.parameter;
			if (p.isInputParameter) {
				value = p.value;
				uncertainty = p.uncertainty;
			} else {
				formula = p.formula;
			}
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(
				"Set a new value for " + param.parameter.name);
		}

		@Override
		protected Point getInitialSize() {
			return new Point(500, 300);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var tk = mform.getToolkit();
			var body = UI.formBody(mform.getForm(), tk);
			var comp = tk.createComposite(body);

			UI.gridData(comp, true, false);
			UI.gridLayout(comp, 2);

			var p = param.parameter;

			// label & text
			tk.createLabel(comp, p.isInputParameter
				? M.Value
				: M.Formula);
			var text = tk.createText(comp, "");
			UI.gridData(text, true, false);
			Controls.onPainted(text, () -> text.setText(
				p.isInputParameter
					? Double.toString(value)
					: formula == null ? "" : formula));

			// uncertainty panel for input parameters
			if (p.isInputParameter) {
				tk.createLabel(comp, M.Uncertainty);
				var link = tk.createHyperlink(
					comp, Uncertainty.string(p.uncertainty), SWT.NONE);
				UI.gridData(link, true, false);

				Controls.onClick(link, $ -> {
					var u = UncertaintyDialog.open(uncertainty).orElse(null);
					if (u == null)
						return;
					uncertainty = u;
					link.setText(Uncertainty.string(u));
					link.getParent().layout();
				});
			}

			// error message
			UI.filler(comp, tk);
			var errorLabel = tk.createLabel(comp, "");
			errorLabel.setForeground(Colors.systemColor(SWT.COLOR_RED));
			Consumer<String> onError = err -> {
				errorLabel.setText(err == null ? "" : err);
				errorLabel.getParent().layout();
				var ok = getButton(IDialogConstants.OK_ID);
				ok.setEnabled(err == null);
			};

			// handle text editing
			text.addModifyListener($ -> {
				var textVal = text.getText();

				if (p.isInputParameter) {
					try {
						value = Double.parseDouble(textVal);
						onError.accept(null);
					} catch (Exception e) {
						onError.accept(textVal + " " + M.IsNotValidNumber);
					}
				} else {

					var scope = param.isGlobal()
						? interpreter.getGlobalScope()
						: interpreter.getScopeOrGlobal(param.ownerId());
					try {
						scope.eval(textVal);
						formula = textVal;
						onError.accept(null);
					} catch (Exception e) {
						onError.accept("Formula error: " + Strings.cut(e.getMessage(), 80));
					}
				}
			});
		}
	}


}
