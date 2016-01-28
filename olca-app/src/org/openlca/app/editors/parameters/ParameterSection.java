package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Error;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.Warning;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.table.modify.ComboBoxCellModifier;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.app.viewers.table.modify.field.DoubleModifier;
import org.openlca.app.viewers.table.modify.field.StringModifier;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.util.Strings;

/**
 * A section with a table for parameters in processes, LCIA methods, and global
 * parameters. It is possible to create two kinds of tables with this class: for
 * input parameters with the columns name, value, and description, or for
 * dependent parameters with the columns name, formula, value, and description.
 */
public class ParameterSection {

	private TableViewer viewer;

	private final String NAME = M.Name;
	private final String VALUE = M.Value;
	private final String FORMULA = M.Formula;
	private final String UNCERTAINTY = M.Uncertainty;
	private final String DESCRIPTION = M.Description;
	private final String EXTERNAL_SOURCE = M.ExternalSource;

	private boolean forInputParameters = true;
	private ParameterChangeSupport support;
	private ModelEditor<? extends CategorizedEntity> editor;
	private Supplier<List<Parameter>> supplier;
	private ParameterScope scope;
	private SourceHandler sourceHandler;

	public static ParameterSection forInputParameters(ModelEditor<? extends CategorizedEntity> editor,
			ParameterChangeSupport support, Composite body,
			FormToolkit toolkit, SourceHandler sourceHandler) {
		return new ParameterSection(editor, support, body, toolkit,
				sourceHandler, true);
	}

	public static ParameterSection forInputParameters(ModelEditor<? extends CategorizedEntity> editor,
			ParameterChangeSupport support, Composite body, FormToolkit toolkit) {
		return new ParameterSection(editor, support, body, toolkit, null, true);
	}

	public static ParameterSection forDependentParameters(ModelEditor<? extends CategorizedEntity> editor,
			ParameterChangeSupport support, Composite body, FormToolkit toolkit) {
		return new ParameterSection(editor, support, body, toolkit, null, false);
	}

	private ParameterSection(ModelEditor<? extends CategorizedEntity> editor, ParameterChangeSupport support,
			Composite body, FormToolkit toolkit, SourceHandler sourceHandler,
			boolean forInputParameters) {
		this.forInputParameters = forInputParameters;
		this.sourceHandler = sourceHandler;
		this.editor = editor;
		this.support = support;
		String[] props = getProperties();
		createComponents(body, toolkit, props);
		createCellModifiers();
		addDoubleClickHandler();
		support.afterEvaluation(this::setInput);
	}

	private String[] getProperties() {
		if (forInputParameters)
			if (sourceHandler != null)
				return new String[] { NAME, VALUE, UNCERTAINTY, DESCRIPTION,
						EXTERNAL_SOURCE };
			else
				return new String[] { NAME, VALUE, UNCERTAINTY, DESCRIPTION };
		else {
			if (sourceHandler != null)
				return new String[] { NAME, FORMULA, VALUE, DESCRIPTION,
						EXTERNAL_SOURCE };
			else
				return new String[] { NAME, FORMULA, VALUE, DESCRIPTION };
		}
	}

	public void setSupplier(Supplier<List<Parameter>> supplier,
			ParameterScope scope) {
		this.supplier = supplier;
		this.scope = scope;
		fillInitialInput();
	}

	private void addDoubleClickHandler() {
		Tables.onDoubleClick(viewer, (event) -> {
			TableItem item = Tables.getItem(viewer, event);
			if (item == null)
				onAdd();
		});
	}

	private void createComponents(Composite body, FormToolkit toolkit,
			String[] properties) {
		String title = forInputParameters ? M.InputParameters
				: M.DependentParameters;
		Section section = UI.section(body, toolkit, title);
		UI.gridData(section, true, true);
		Composite parent = UI.sectionClient(section, toolkit);
		viewer = Tables.createViewer(parent, properties);
		ParameterLabelProvider label = new ParameterLabelProvider();
		viewer.setLabelProvider(label);
		addSorters(viewer, label);
		bindColumnWidths(viewer);
		bindActions(section);
	}

	private void bindColumnWidths(TableViewer viewer) {
		if (sourceHandler != null)
			Tables.bindColumnWidths(viewer, 0.25, 0.25, 0.15, 0.15, 0.2);
		else
			Tables.bindColumnWidths(viewer, 0.3, 0.3, 0.2, 0.2);

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
		Actions.bind(section, addAction, removeAction);
		Actions.bind(viewer, addAction, removeAction, copy, paste);
		Tables.onDeletePressed(viewer, (e) -> onRemove());
	}

	private void createCellModifiers() {
		ModifySupport<Parameter> ms = new ModifySupport<>(viewer);
		ms.bind(NAME, new NameModifier());
		ms.bind(DESCRIPTION, new StringModifier<>(editor, "description"));
		if (forInputParameters) {
			ms.bind(VALUE, new DoubleModifier<>(editor, "value",
					(elem) -> support.evaluate()));
			ms.bind(UNCERTAINTY, new UncertaintyCellEditor(viewer.getTable(),
					editor));
		} else
			ms.bind(FORMULA, new StringModifier<>(editor, "formula",
					(elem) -> support.evaluate()));
		if (sourceHandler != null)
			ms.bind(EXTERNAL_SOURCE, new ExternalSourceModifier());
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
		p.setScope(scope);
		p.setInputParameter(forInputParameters);
		p.setValue(1.0);
		if (!forInputParameters)
			p.setFormula("1.0");
		params.add(p);
		setInput();
		editor.setDirty(true);
	}

	private boolean exists(String name) {
		for (Parameter parameter : supplier.get())
			if (Strings.nullOrEqual(name, parameter.getName()))
				return true;
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
		List<Parameter> params = forInputParameters ?
				Clipboard.readInputParams(text) : Clipboard.readCalculatedParams(text);
		boolean skipped = false;
		for (Parameter param : params) {
			String name = param.getName();
			if (!Parameter.isValidName(name) || exists(name)) {
				skipped = true;
				continue;
			}
			param.setScope(scope);
			supplier.get().add(param);
		}
		if (skipped)
			Warning.showBox("#Some parameters were not added because their names were either invalid or a parameter with the same name already existed.");
		setInput();
		editor.setDirty(true);
		support.evaluate();
	}

	private class ParameterLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int col) {
			if (col != 0 || !(element instanceof Parameter))
				return null;
			Parameter parameter = (Parameter) element;
			if (parameter.getExternalSource() == null)
				return null;
			// currently the only external sources are shape files
			return Images.get(ModelType.IMPACT_METHOD); 
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof Parameter))
				return null;
			Parameter parameter = (Parameter) element;
			switch (columnIndex) {
			case 0:
				return parameter.getName();
			case 1:
				if (forInputParameters)
					return Double.toString(parameter.getValue());
				else
					return parameter.getFormula();
			case 2:
				if (forInputParameters)
					return UncertaintyLabel.get(parameter.getUncertainty());
				else
					return Double.toString(parameter.getValue());
			case 3:
				return parameter.getDescription();
			case 4:
				return parameter.getExternalSource();
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
						"#A parameter with the same name already exists");
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
			return sourceHandler.getSources(element);
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
			if (!Question.ask("#External source change",
					"#Values will be recalculated, do you want to proceed?"))
				return;
			element.setExternalSource(item);
			sourceHandler.sourceChanged(element, item);
		}

	}

}
