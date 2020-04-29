package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.FormulaCellEditor;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.db.Database;
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
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.app.viewers.table.modify.field.StringModifier;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Uncertainty;
import org.openlca.formula.Formulas;
import org.openlca.util.Strings;

/**
 * A section with a table for parameters in processes, LCIA methods, and global
 * parameters. It is possible to create two kinds of tables with this class: for
 * input parameters with the columns name, value, and description, or for
 * dependent parameters with the columns name, formula, value, and description.
 */
public class ParameterSection {

	private TableViewer table;
	private boolean forInputParameters = true;
	private final ParameterPage<?> page;
	private final ModelEditor<?> editor;
	private ParameterChangeSupport support;

	static ParameterSection forInputParameters(ParameterPage<?> page) {
		return new ParameterSection(page, true);
	}

	static ParameterSection forDependentParameters(ParameterPage<?> page) {
		return new ParameterSection(page, false);
	}

	private ParameterSection(ParameterPage<?> page, boolean forInputParameters) {
		this.forInputParameters = forInputParameters;
		this.editor = page.editor;
		this.page = page;
		this.support = page.support;
		String[] props = getProperties();
		createComponents(page.body, page.toolkit, props);
		createCellModifiers();
		addDoubleClickHandler();
		support.afterEvaluation(this::setInput);
		editor.onSaved(this::setInput);
		fillInitialInput();
	}

	private String[] getProperties() {
		var props = forInputParameters
				? List.of(M.Name, M.Value, M.Uncertainty, M.Description)
				: List.of(M.Name, M.Formula, M.Value, M.Description);
		if (editor.hasAnyComment("parameters")) {
			props = new ArrayList<>(props);
			props.add("");
		}
		return props.toArray(new String[0]);
	}

	private void addDoubleClickHandler() {
		Tables.onDoubleClick(table, (event) -> {
			TableItem item = Tables.getItem(table, event);
			if (item == null)
				onAdd();
		});
	}

	private void createComponents(Composite body, FormToolkit toolkit, String[] properties) {
		String title = forInputParameters ? M.InputParameters : M.DependentParameters;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		Composite parent = UI.sectionClient(section, toolkit, 1);
		table = Tables.createViewer(parent, properties);
		ParameterLabelProvider label = new ParameterLabelProvider();
		table.setLabelProvider(label);
		addSorters(table, label);
		bindActions(section);
		Tables.bindColumnWidths(table, 0.3, 0.3, 0.2, 0.17, 0.03);
		int col = forInputParameters ? 1 : 2;
		table.getTable().getColumns()[col].setAlignment(SWT.RIGHT);
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
		var add = Actions.onAdd(() -> onAdd());
		var remove = Actions.onRemove(() -> onRemove());
		var copy = TableClipboard.onCopy(table);
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
		var ms = new ModifySupport<Parameter>(table);
		ms.bind(M.Name, new NameModifier());
		ms.bind(M.Description, new StringModifier<>(editor, "description"));
		ms.bind(M.Value, new DoubleModifier<>(editor, "value", (elem) -> support.evaluate()));
		ms.bind(M.Uncertainty, new UncertaintyCellEditor(table.getTable(), editor));
		ms.bind("", new CommentDialogModifier<Parameter>(editor.getComments(), CommentPaths::get));
		var formulaEditor = new FormulaCellEditor(table, () -> page.parameters());
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

	private void fillInitialInput() {
		Collections.sort(page.parameters(),
				(o1, o2) -> Strings.compare(o1.name, o2.name));
		setInput();
	}

	private void setInput() {
		List<Parameter> input = new ArrayList<>();
		for (var param : page.parameters()) {
			if (param.isInputParameter == forInputParameters) {
				input.add(param);
			}
		}
		table.setInput(input);
	}

	private void onAdd() {
		var params = page.parameters();
		int count = params.size();
		String name = "p_" + count++;
		while (exists(name))
			name = "p_" + count++;
		var p = new Parameter();
		p.refId = UUID.randomUUID().toString();
		p.name = name;
		p.scope = page.scope;
		p.isInputParameter = forInputParameters;
		p.value = 1.0;
		if (!forInputParameters)
			p.formula = "1.0";
		params.add(p);
		setInput();
		editor.setDirty(true);
	}

	private boolean exists(String name) {
		for (var param : page.parameters()) {
			if (name == null && param.name == null)
				return true;
			if (name == null || param.name == null)
				continue;
			if (name.toLowerCase().equals(param.name.toLowerCase()))
				return true;
		}
		return false;
	}

	private void onRemove() {
		List<Parameter> params = page.parameters();
		List<Parameter> selection = Viewers.getAllSelected(table);
		for (Parameter parameter : selection) {
			params.remove(parameter);
		}
		setInput();
		editor.setDirty(true);
		support.evaluate();
	}

	private void onPaste(String text) {
		List<Parameter> params = forInputParameters
				? Clipboard.readAsInputParams(text, page.scope)
				: Clipboard.readAsCalculatedParams(text, page.scope);
		boolean skipped = false;
		for (Parameter param : params) {
			String name = param.name;
			if (!Parameter.isValidName(name) || exists(name)) {
				skipped = true;
				continue;
			}
			page.parameters().add(param);
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
			if (!Parameter.isValidName(name)) {
				MsgBox.error(M.InvalidParameterName, name + " "
						+ M.IsNotValidParameterName);
				return;
			}
			if (exists(name)) {
				MsgBox.error(M.InvalidParameterName,
						M.ParameterWithSameNameExists);
				return;
			}
			param.name = name;
			editor.setDirty(true);
			support.evaluate();
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
			page.parameters().remove(param);
			var global = param.clone();
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
				for (var localParam : page.parameters()) {
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
