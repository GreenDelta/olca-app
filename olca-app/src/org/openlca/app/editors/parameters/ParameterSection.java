package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.comments.CommentAction;
import org.openlca.app.editors.comments.CommentDialogModifier;
import org.openlca.app.editors.comments.CommentPaths;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.util.Warning;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.app.viewers.table.modify.field.StringModifier;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.Uncertainty;
import org.openlca.util.Strings;

/**
 * A section with a table for parameters in processes, LCIA methods, and global
 * parameters. It is possible to create two kinds of tables with this class: for
 * input parameters with the columns name, value, and description, or for
 * dependent parameters with the columns name, formula, value, and description.
 */
public class ParameterSection {

	private TableViewer viewer;
	private boolean forInputParameters = true;
	private final ParameterPage<?> page;
	private final ModelEditor<?> editor;
	private final Supplier<List<Parameter>> supplier;
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
		this.supplier = page.supplier;
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
		List<String> props;
		if (forInputParameters) {
			props = new ArrayList<>(Arrays.asList(
					M.Name, M.Value, M.Uncertainty, M.Description));
		} else {
			props = new ArrayList<>(Arrays.asList(
					M.Name, M.Formula, M.Value, M.Description));
		}
		if (page.sourceHandler != null)
			props.add(M.ExternalSource);
		if (editor.hasAnyComment("parameters"))
			props.add("");
		return props.toArray(new String[props.size()]);
	}

	private void addDoubleClickHandler() {
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private void createComponents(Composite body, FormToolkit toolkit, String[] properties) {
		String title = forInputParameters ? M.InputParameters : M.DependentParameters;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		Composite parent = UI.sectionClient(section, toolkit, 1);
		viewer = Tables.createViewer(parent, properties);
		ParameterLabelProvider label = new ParameterLabelProvider();
		viewer.setLabelProvider(label);
		addSorters(viewer, label);
		bindActions(section);
		if (page.sourceHandler != null)
			Tables.bindColumnWidths(viewer, 0.25, 0.25, 0.15, 0.15, 0.17);
		else {
			Tables.bindColumnWidths(viewer, 0.3, 0.3, 0.2, 0.17, 0.03);
		}
		int col = forInputParameters ? 1 : 2;
		viewer.getTable().getColumns()[col].setAlignment(SWT.RIGHT);
	}

	private void addSorters(TableViewer table, ParameterLabelProvider label) {
		if (forInputParameters) {
			Viewers.sortByLabels(table, label, 0, 2, 3);
			Viewers.sortByDouble(table, (Parameter p) -> p.getValue(), 1);
		} else {
			Viewers.sortByLabels(table, label, 0, 1, 3);
			Viewers.sortByDouble(table, (Parameter p) -> p.getValue(), 2);
		}
	}

	private void bindActions(Section section) {
		Action addAction = Actions.onAdd(() -> onAdd());
		Action removeAction = Actions.onRemove(() -> onRemove());
		Action copy = TableClipboard.onCopy(viewer);
		Action paste = TableClipboard.onPaste(viewer, this::onPaste);
		CommentAction.bindTo(section, "parameters",
				editor.getComments(), addAction, removeAction);
		Actions.bind(viewer, addAction, removeAction, copy, paste);
		Tables.onDeletePressed(viewer, (e) -> onRemove());
	}

	private void createCellModifiers() {
		ModelEditor<?> editor = page.editor;
		ModifySupport<Parameter> ms = new ModifySupport<>(viewer);
		ms.bind(M.Name, new NameModifier());
		ms.bind(M.Description, new StringModifier<>(editor, "description"));
		ms.bind(M.Value, new DoubleModifier<>(editor, "value", (elem) -> support.evaluate()));
		ms.bind(M.Uncertainty, new UncertaintyCellEditor(viewer.getTable(), editor));
		ms.bind(M.ExternalSource, new ExternalSourceModifier());
		ms.bind("", new CommentDialogModifier<Parameter>(editor.getComments(), CommentPaths::get));
		FormulaCellEditor formulaEditor = new FormulaCellEditor(viewer, supplier);
		ms.bind(M.Formula, formulaEditor);
		formulaEditor.onEdited((obj, formula) -> {
			if (!(obj instanceof Parameter))
				return;
			Parameter param = (Parameter) obj;
			param.setFormula(formula);
			support.evaluate();
			viewer.refresh();
			editor.setDirty(true);
		});
	}

	private void fillInitialInput() {
		if (supplier == null)
			return;
		Collections.sort(supplier.get(),
				(o1, o2) -> Strings.compare(o1.getName(), o2.getName()));
		setInput();
	}

	private void setInput() {
		if (supplier == null)
			return;
		List<Parameter> input = new ArrayList<>();
		for (Parameter param : supplier.get()) {
			if (param.isInputParameter() == forInputParameters)
				input.add(param);
		}
		viewer.setInput(input);
	}

	private void onAdd() {
		if (supplier == null)
			return;
		List<Parameter> params = supplier.get();
		int count = params.size();
		String name = "p_" + count++;
		while (exists(name))
			name = "p_" + count++;
		Parameter p = new Parameter();
		p.setRefId(UUID.randomUUID().toString());
		p.setName(name);
		p.setScope(page.scope);
		p.setInputParameter(forInputParameters);
		p.setValue(1.0);
		if (!forInputParameters)
			p.setFormula("1.0");
		params.add(p);
		setInput();
		editor.setDirty(true);
	}

	private boolean exists(String name) {
		for (Parameter parameter : supplier.get()) {
			if (name == null && parameter.getName() == null)
				return true;
			if (name == null || parameter.getName() == null)
				continue;
			if (name.toLowerCase().equals(parameter.getName().toLowerCase()))
				return true;
		}
		return false;
	}

	private void onRemove() {
		if (supplier == null)
			return;
		List<Parameter> params = supplier.get();
		List<Parameter> selection = Viewers.getAllSelected(viewer);
		for (Parameter parameter : selection) {
			params.remove(parameter);
		}
		setInput();
		editor.setDirty(true);
		support.evaluate();
	}

	private void onPaste(String text) {
		if (supplier == null)
			return;
		List<Parameter> params = forInputParameters
				? Clipboard.readAsInputParams(text, page.scope)
				: Clipboard.readAsCalculatedParams(text, page.scope);
		boolean skipped = false;
		for (Parameter param : params) {
			String name = param.getName();
			if (!Parameter.isValidName(name) || exists(name)) {
				skipped = true;
				continue;
			}
			supplier.get().add(param);
		}
		if (skipped) {
			Warning.showBox(M.SomeParametersWereNotAdded);
		}
		setInput();
		editor.setDirty(true);
		support.evaluate();
	}

	private class ParameterLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int col) {
			Parameter parameter = (Parameter) element;
			if (col == 0) {
				if (parameter.getExternalSource() == null)
					return null;
				// currently the only external sources are shape files
				return Images.get(ModelType.IMPACT_METHOD);
			} else if (col == getProperties().length - 1 && editor.hasAnyComment("parameters"))
				return Images.get(editor.getComments(), CommentPaths.get(parameter));
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Parameter))
				return null;
			Parameter p = (Parameter) obj;
			switch (col) {
			case 0:
				return p.getName();
			case 1:
				if (forInputParameters)
					return Double.toString(p.getValue());
				else
					return p.getFormula();
			case 2:
				if (forInputParameters)
					return Uncertainty.string(p.getUncertainty());
				else
					return Double.toString(p.getValue());
			case 3:
				return p.getDescription();
			case 4:
				return p.getExternalSource();
			default:
				return null;
			}
		}
	}

	private class NameModifier extends TextCellModifier<Parameter> {
		@Override
		protected String getText(Parameter param) {
			return param.getName();
		}

		@Override
		protected void setText(Parameter param, String text) {
			if (text == null)
				return;
			if (Objects.equals(text, param.getName()))
				return;
			String name = text.trim();
			if (!Parameter.isValidName(name)) {
				Error.showBox(M.InvalidParameterName, name + " "
						+ M.IsNotValidParameterName);
				return;
			}
			if (exists(name)) {
				Error.showBox(M.InvalidParameterName,
						M.ParameterWithSameNameExists);
				return;
			}
			param.setName(name);
			editor.setDirty(true);
			support.evaluate();
		}
	}

	private class ExternalSourceModifier extends
			ComboBoxCellModifier<Parameter, String> {

		@Override
		protected String[] getItems(Parameter element) {
			return page.sourceHandler.getSources(element);
		}

		@Override
		protected String getItem(Parameter element) {
			return element.getExternalSource();
		}

		@Override
		protected String getText(String value) {
			return value;
		}

		@Override
		protected void setItem(Parameter element, String item) {
			if (!Question.ask(M.ExternalSourceChange, M.RecalculateQuestion))
				return;
			element.setExternalSource(item);
			page.sourceHandler.sourceChanged(element, item);
		}
	}
}
