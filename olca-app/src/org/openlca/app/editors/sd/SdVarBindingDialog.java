package org.openlca.app.editors.sd;

import java.util.List;
import java.util.Optional;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.openlca.app.editors.sd.interop.VarBinding;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.Parameter;
import org.openlca.sd.eqn.Id;
import org.openlca.util.Strings;

class SdVarBindingDialog extends FormDialog {

	private final VarBinding binding;
	private final List<Id> modelVariables;
	private final List<Parameter> systemParameters;

	private TableViewer variablesTable;
	private TableViewer parametersTable;
	private Text variablesFilter;
	private Text parametersFilter;

	public static Optional<VarBinding> create(
			List<Id> modelVariables,
			List<Parameter> systemParameters) {
		var binding = new VarBinding();
		var dialog = new SdVarBindingDialog(binding, modelVariables, systemParameters);
		return dialog.open() == OK
			? Optional.of(binding)
			: Optional.empty();
	}

	public static boolean edit(
			VarBinding binding,
			List<Id> modelVariables,
			List<Parameter> systemParameters) {
		if (binding == null)
			return false;
		var dialog = new SdVarBindingDialog(binding, modelVariables, systemParameters);
		return dialog.open() == OK;
	}

	private SdVarBindingDialog(
			VarBinding binding,
			List<Id> modelVariables,
			List<Parameter> systemParameters) {
		super(UI.shell());
		this.binding = binding;
		this.modelVariables = modelVariables;
		this.systemParameters = systemParameters;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Variable binding");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		UI.gridLayout(body, 2, 10, 0);

		createVariablesSection(body, tk);
		createParametersSection(body, tk);
	}

	private void createVariablesSection(org.eclipse.swt.widgets.Composite body,
			org.eclipse.ui.forms.widgets.FormToolkit tk) {
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);

		UI.label(comp, tk, "Model variable");

		variablesFilter = UI.text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(variablesFilter, true, false);
		variablesFilter.setMessage("Search filter");

		variablesTable = new TableViewer(comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		UI.gridData(variablesTable.getControl(), true, true);
		variablesTable.setContentProvider(ArrayContentProvider.getInstance());
		variablesTable.setLabelProvider(new VariableLabelProvider());
		variablesTable.setInput(modelVariables);

		// Select current variable if set
		if (binding.varId() != null) {
			// variablesTable.setSelection(Viewers.structuredSelection(binding.varId()));
		}

		// Handle selection
		variablesTable.addSelectionChangedListener(e -> {
			var selected = Viewers.getFirstSelected(variablesTable);
			if (selected instanceof Id id) {
				binding.varId(id);
			}
		});

		// Handle filter
		variablesFilter.addModifyListener(e -> {
			var filterText = variablesFilter.getText().toLowerCase();
			if (Strings.nullOrEmpty(filterText)) {
				variablesTable.setInput(modelVariables);
			} else {
				var filtered = modelVariables.stream()
					.filter(id -> id.label().toLowerCase().contains(filterText))
					.toList();
				variablesTable.setInput(filtered);
			}
		});
	}

	private void createParametersSection(org.eclipse.swt.widgets.Composite body,
			org.eclipse.ui.forms.widgets.FormToolkit tk) {
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);

		UI.label(comp, tk, "System parameter");

		parametersFilter = UI.text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(parametersFilter, true, false);
		parametersFilter.setMessage("Search filter");

		parametersTable = new TableViewer(comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		UI.gridData(parametersTable.getControl(), true, true);
		parametersTable.setContentProvider(ArrayContentProvider.getInstance());
		parametersTable.setLabelProvider(new ParameterLabelProvider());
		parametersTable.setInput(systemParameters);

		// Select current parameter if set
		if (binding.parameter() != null) {
			// parametersTable.setSelection(Viewers.structuredSelection(binding.parameter()));
		}

		// Handle selection
		parametersTable.addSelectionChangedListener(e -> {
			var selected = Viewers.getFirstSelected(parametersTable);
			if (selected instanceof Parameter param) {
				binding.parameter(param);
			}
		});

		// Handle filter
		parametersFilter.addModifyListener(e -> {
			var filterText = parametersFilter.getText().toLowerCase();
			if (Strings.nullOrEmpty(filterText)) {
				parametersTable.setInput(systemParameters);
			} else {
				var filtered = systemParameters.stream()
					.filter(p -> p.name != null && p.name.toLowerCase().contains(filterText))
					.toList();
				parametersTable.setInput(filtered);
			}
		});
	}

	@Override
	protected void okPressed() {
		if (binding.varId() == null || binding.parameter() == null) {
			// Could show error message if needed
			return;
		}
		super.okPressed();
	}

	private static class VariableLabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			return element instanceof Id id ? id.value() : "";
		}
	}

	private static class ParameterLabelProvider extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof Parameter param) {
				return param.name + (Strings.notEmpty(param.description)
					? " - " + param.description
					: "");
			}
			return "";
		}
	}
}
