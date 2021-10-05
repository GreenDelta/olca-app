package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.app.viewers.tables.modify.ModifySupport;
import org.openlca.app.viewers.tables.modify.TextCellModifier;
import org.openlca.app.viewers.tables.modify.field.DoubleModifier;
import org.openlca.app.viewers.tables.modify.field.StringModifier;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.ParameterizedEntity;
import org.openlca.core.model.Uncertainty;
import org.openlca.formula.Formulas;
import org.openlca.util.Parameters;
import org.openlca.util.Strings;

/**
 * A section with a table for parameters in processes, LCIA methods, and global
 * parameters. It is possible to create two kinds of tables with this class: for
 * input parameters with the columns name, value, and description, or for
 * dependent parameters with the columns name, formula, value, and description.
 */
public class ParameterSection {

	private TableViewer table;
	private final boolean forInputParameters;
	private final ParameterPage<?> page;
	private final ModelEditor<? extends ParameterizedEntity> editor;
	private final ParameterChangeSupport support;

	static void forInputParameters(ParameterPage<?> page) {
		new ParameterSection(page, true);
	}

	static void forDependentParameters(ParameterPage<?> page) {
		new ParameterSection(page, false);
	}

	private ParameterSection(ParameterPage<?> page, boolean forInputParameters) {
		this.forInputParameters = forInputParameters;
		this.editor = page.editor;
		this.page = page;
		this.support = page.support;
		createComponents(page.body, page.toolkit);
		createCellModifiers();
		support.afterEvaluation(this::setInput);
		editor.onSaved(this::setInput);
		entity().parameters.sort((o1, o2) -> Strings.compare(o1.name, o2.name));
		setInput();
	}

	private ParameterizedEntity entity() {
		return editor.getModel();
	}

	private void createComponents(Composite body, FormToolkit toolkit) {
		String title = forInputParameters ? M.InputParameters : M.DependentParameters;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		Composite parent = UI.sectionClient(section, toolkit, 1);
		table = Tables.createViewer(parent, columns());
		ParameterLabelProvider label = new ParameterLabelProvider();
		table.setLabelProvider(label);
		addSorters(table, label);
		bindActions(section);
		Tables.bindColumnWidths(table, 0.3, 0.3, 0.2, 0.17, 0.03);
		int col = forInputParameters ? 1 : 2;
		table.getTable().getColumns()[col].setAlignment(SWT.RIGHT);
		Tables.onDoubleClick(table, e -> {
			var item = Tables.getItem(table, e);
			if (item == null) {
				onAdd();
			}
		});
	}

	private String[] columns() {
		var props = forInputParameters
				? List.of(M.Name, M.Value, M.Uncertainty, M.Description)
				: List.of(M.Name, M.Formula, M.Value, M.Description);
		if (editor.hasAnyComment("parameters")) {
			props = new ArrayList<>(props);
			props.add("");
		}
		return props.toArray(new String[0]);
	}

	private void addSorters(TableViewer table, ParameterLabelProvider label) {
		if (forInputParameters) {
			Viewers.sortByLabels(table, label, 0, 2, 3);
			Viewers.sortByDouble(table, (Parameter p) -> p.value, 1);
		} else {
			Viewers.sortByLabels(table, label, 0, 1, 3);
			Viewers.sortByDouble(table, (Parameter p) -> p.value, 2);
		}
	}

	private void bindActions(Section section) {
		if (!editor.isEditable())
			return;
		var add = Actions.onAdd(this::onAdd);
		var remove = Actions.onRemove(this::onRemove);
		var copy = TableClipboard.onCopySelected(table);
		var paste = TableClipboard.onPaste(table, this::onPaste);
		var usage = Actions.create(M.Usage, Icon.LINK.descriptor(), () -> {
			Parameter p = Viewers.getFirstSelected(table);
			if (p != null) {
				ParameterUsagePage.show(p.name);
			}
		});
		var toGlobal = new ConvertToGlobalAction();
		CommentAction.bindTo(section, "parameters",
				editor.getComments(), add, remove);
		Actions.bind(table, add, remove, copy, paste, usage, toGlobal);
		Tables.onDeletePressed(table, (e) -> onRemove());
	}

	private void createCellModifiers() {
		var editor = page.editor;
		if (!editor.isEditable())
			return;
		var ms = new ModifySupport<Parameter>(table);
		ms.bind(M.Name, new NameModifier());
		ms.bind(M.Description, new StringModifier<>(editor, "description"));
		ms.bind(M.Value, new DoubleModifier<>(editor, "value", (elem) -> support.evaluate()));
		ms.bind(M.Uncertainty, new UncertaintyCellEditor(table.getTable(), editor));
		ms.bind("", new CommentDialogModifier<>(editor.getComments(), CommentPaths::get));
		var formulaEditor = new FormulaCellEditor(table, () -> entity().parameters);
		ms.bind(M.Formula, formulaEditor);
		formulaEditor.onEdited((obj, formula) -> {
			if (!(obj instanceof Parameter))
				return;
			Parameter param = (Parameter) obj;
			param.formula = formula;
			support.evaluate();
			table.refresh();
			editor.setDirty(true);
		});
	}

	private void setInput() {
		List<Parameter> input = new ArrayList<>();
		for (var param : entity().parameters) {
			if (param.isInputParameter == forInputParameters) {
				input.add(param);
			}
		}
		table.setInput(input);
	}

	private void onAdd() {
		var e = entity();
		var params = e.parameters;
		int count = params.size() + 1;
		String name = "p_" + count;
		while (exists(name)) {
			count++;
			name = "p_" + count;
		}
		if (forInputParameters) {
			e.parameter(name, 1.0);
		} else {
			e.parameter(name, "1.0");
		}
		setInput();
		editor.setDirty(true);
	}

	private boolean exists(String name) {
		if (name == null)
			return false;
		var _name = name.trim().toLowerCase();
		for (var param : entity().parameters) {
			var other = param.name;
			if (other == null)
				continue;
			if (_name.equals(other.trim().toLowerCase()))
				return true;
		}
		return false;
	}

	private void onRemove() {
		// TODO: give a hint when the parameter is used in a redefinition
		var params = entity().parameters;
		List<Parameter> selection = Viewers.getAllSelected(table);
		if (selection.isEmpty())
			return;
		if (!params.removeAll(selection))
			return;
		setInput();
		editor.setDirty(true);
		support.evaluate();
	}

	private void onPaste(String text) {
		var scope = entity().parameterScope();
		var params = forInputParameters
				? Clipboard.readAsInputParams(text, scope)
				: Clipboard.readAsCalculatedParams(text, scope);
		boolean skipped = false;
		for (Parameter param : params) {
			String name = param.name;
			if (!Parameters.isValidName(name) || exists(name)) {
				skipped = true;
				continue;
			}
			entity().parameters.add(param);
		}
		if (skipped) {
			MsgBox.warning(M.SomeParametersWereNotAdded);
		}
		setInput();
		editor.setDirty(true);
		support.evaluate();
	}

	private class ParameterLabelProvider extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Parameter))
				return null;
			Parameter param = (Parameter) obj;
			int n = table.getTable().getColumnCount();
			if (col == n && editor.hasAnyComment("parameters"))
				return Images.get(editor.getComments(), CommentPaths.get(param));
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Parameter))
				return null;
			Parameter p = (Parameter) obj;
			switch (col) {
			case 0:
				return p.name;
			case 1:
				if (forInputParameters)
					return Double.toString(p.value);
				else
					return p.formula;
			case 2:
				if (forInputParameters)
					return Uncertainty.string(p.uncertainty);
				else
					return Double.toString(p.value);
			case 3:
				return p.description;
			default:
				return null;
			}
		}
	}

	private class NameModifier extends TextCellModifier<Parameter> {
		@Override
		protected String getText(Parameter param) {
			return param.name;
		}

		@Override
		protected void setText(Parameter param, String text) {
			if (text == null)
				return;
			if (Objects.equals(text, param.name))
				return;

			String name = text.trim();

			// check the parameter name
			if (!Parameters.isValidName(name)) {
				MsgBox.error(M.InvalidParameterName, name + " "
						+ M.IsNotValidParameterName);
				return;
			}
			if (exists(name)) {
				MsgBox.error(M.InvalidParameterName,
						M.ParameterWithSameNameExists);
				return;
			}

			boolean isUsed = Parameters.isUsed(
					param, entity(), Database.get());
			if (!isUsed) {
				param.name = name;
				editor.setDirty(true);
				support.evaluate();
				return;
			}

			boolean b = Question.ask("Rename parameter?",
					"The parameter is already used." +
					"This will rename the parameter where it " +
					"is used and save the data set. " +
					"Should we do that?");
			if (!b)
				return;

			// save & close the entity, rename it, and reopen it
			try {
				editor.setDirty(true);
				var page = Editors.getActivePage();
				if (page == null) {
					editor.doSave(null);
				} else {
					page.saveEditor(editor, false);
					page.closeEditor(editor, false);
				}
				var entity = Parameters.rename(
						param, entity(), Database.get(), name);
				App.open(entity);
			} catch (Exception e) {
				MsgBox.error("Renaming failed: " + e.getMessage());
			}
		}
	}

	private class ConvertToGlobalAction extends Action {

		ConvertToGlobalAction() {
			setText("Convert to global parameter");
			setToolTipText("Convert to global parameter");
			setImageDescriptor(Icon.UP.descriptor());
		}

		@Override
		public void run() {

			// check
			Parameter param = Viewers.getFirstSelected(table);
			if (param == null)
				return;
			String err = check(param);
			if (err != null) {
				MsgBox.info("Cannot be converted to global parameter", err);
				return;
			}

			// ask
			boolean b = Question.ask(
					"Convert to global parameter?",
					"Do you want to convert the selected parameter `"
							+ param.name + "` into a global parameter?");
			if (!b)
				return;

			// do it
			entity().parameters.remove(param);
			var global = param.copy();
			global.scope = ParameterScope.GLOBAL;
			new ParameterDao(Database.get()).insert(global);
			page.setGlobalTableInput();
			Navigator.refresh();
			setInput();
			editor.setDirty(true);
		}

		private String check(Parameter param) {

			try {
				var dao = new ParameterDao(Database.get());
				if (dao.existsGlobal(param.name)) {
					return "A global parameter with the name `"
							+ param.name + "` already exists.";
				}

				if (param.isInputParameter)
					return null;

				// check that there are no references to local parameters
				var variables = Formulas.getVariables(param.formula);
				for (var localParam : entity().parameters) {
					if (Objects.equals(localParam, param))
						continue;
					if (localParam.name == null)
						continue;
					String local = localParam.name.trim().toLowerCase();
					for (var variable : variables) {
						if (variable.trim().toLowerCase().equals(local)) {
							return "The parameter `" + param.name
									+ "` cannot be converted into a global"
									+ " parameter as its formula has references"
									+ " to non-global parameters.";
						}
					}
				}

				return null;
			} catch (Exception e) {
				return e.getMessage();
			}
		}
	}
}
