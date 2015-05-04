package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.components.UncertaintyCellEditor;
import org.openlca.app.editors.IEditor;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Dialog;
import org.openlca.app.util.Error;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.table.modify.ModifySupport;
import org.openlca.app.viewers.table.modify.TextCellModifier;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.util.Strings;

/**
 * A section with a table for parameters in processes and LCIA methods. It is
 * possible to create two kinds of tables with this class: for input parameters
 * with the columns name, value, and description, or for dependent parameters
 * with the columns name, formula, value, and description.
 */
public class ParameterSection {

	private TableViewer viewer;

	private final String NAME = Messages.Name;
	private final String VALUE = Messages.Value;
	private final String FORMULA = Messages.Formula;
	private final String UNCERTAINTY = Messages.Uncertainty;
	private final String DESCRIPTION = Messages.Description;

	private boolean forInputParameters = true;
	private ParameterChangeSupport support;
	private IEditor editor;
	private Supplier<List<Parameter>> supplier;
	private ParameterScope scope;

	public static ParameterSection forInputParameters(IEditor editor,
			ParameterChangeSupport support, Composite body, FormToolkit toolkit) {
		return new ParameterSection(editor, support, body, toolkit, true);
	}

	public static ParameterSection forDependentParameters(IEditor editor,
			ParameterChangeSupport support, Composite body, FormToolkit toolkit) {
		return new ParameterSection(editor, support, body, toolkit, false);
	}

	private ParameterSection(IEditor editor, ParameterChangeSupport support,
			Composite body, FormToolkit toolkit, boolean forInputParams) {
		forInputParameters = forInputParams;
		this.editor = editor;
		this.support = support;
		String[] props = {};
		if (forInputParams)
			props = new String[] { NAME, VALUE, UNCERTAINTY, DESCRIPTION };
		else
			props = new String[] { NAME, FORMULA, VALUE, DESCRIPTION };
		createComponents(body, toolkit, props);
		createCellModifiers();
		addDoubleClickHandler();
		support.afterEvaluation(this::setInput);
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
		String label = forInputParameters ? Messages.InputParameters
				: Messages.DependentParameters;
		Section section = UI.section(body, toolkit, label);
		UI.gridData(section, true, true);
		Composite parent = UI.sectionClient(section, toolkit);
		viewer = Tables.createViewer(parent, properties);
		viewer.setLabelProvider(new ParameterLabelProvider());
		Table table = viewer.getTable();
		if (forInputParameters)
			Tables.bindColumnWidths(table, 0.3, 0.3, 0.2, 0.2);
		else
			Tables.bindColumnWidths(table, 0.3, 0.3, 0.2, 0.2);
		bindActions(section);
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
		ModifySupport<Parameter> modifySupport = new ModifySupport<>(viewer);
		modifySupport.bind(NAME, new NameModifier());
		modifySupport.bind(DESCRIPTION, new DescriptionModifier());
		if (forInputParameters) {
			modifySupport.bind(VALUE, new ValueModifier());
			modifySupport.bind(UNCERTAINTY,
					new UncertaintyCellEditor(viewer.getTable(), editor));
		} else
			modifySupport.bind(FORMULA, new FormulaModifier());
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
		Parameter parameter = new Parameter();
		parameter.setName("p_" + params.size());
		parameter.setScope(scope);
		parameter.setInputParameter(forInputParameters);
		parameter.setValue(1.0);
		if (!forInputParameters)
			parameter.setFormula("1.0");
		params.add(parameter);
		setInput();
		editor.setDirty(true);
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
				Clipboard.readInputParams(text)
				: Clipboard.readCalculatedParams(text);
		for (Parameter param : params) {
			param.setScope(scope);
			supplier.get().add(param);
		}
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
			if (parameter.getExternalSource() != null)
				return ImageType.LCIA_ICON.get(); // currently the only external
			// sources are shape files
			else
				return null;
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
				Error.showBox(Messages.InvalidParameterName, name + " "
						+ Messages.IsNotValidParameterName);
				return;
			}
			param.setName(name);
			editor.setDirty(true);
			support.evaluate();
		}
	}

	private class ValueModifier extends TextCellModifier<Parameter> {
		@Override
		protected String getText(Parameter param) {
			return Double.toString(param.getValue());
		}

		@Override
		protected void setText(Parameter param, String text) {
			try {
				double d = Double.parseDouble(text);
				param.setValue(d);
				editor.setDirty(true);
				support.evaluate();
			} catch (Exception e) {
				Dialog.showError(viewer.getTable().getShell(), text
						+ " " + Messages.IsNotValidNumber);
			}
		}
	}

	private class FormulaModifier extends TextCellModifier<Parameter> {
		@Override
		protected String getText(Parameter param) {
			return param.getFormula();
		}

		@Override
		protected void setText(Parameter param, String formula) {
			try {
				param.setFormula(formula);
				editor.setDirty(true);
				support.evaluate();
			} catch (Exception e) {
				Error.showBox(Messages.InvalidFormula,
						Strings.cut(e.getMessage(), 75));
			}
		}
	}

	private class DescriptionModifier extends TextCellModifier<Parameter> {
		@Override
		protected String getText(Parameter param) {
			return param.getDescription();
		}

		@Override
		protected void setText(Parameter param, String text) {
			if (!Objects.equals(text, param.getDescription())) {
				param.setDescription(text);
				editor.setDirty(true);
			}
		}
	}

}
