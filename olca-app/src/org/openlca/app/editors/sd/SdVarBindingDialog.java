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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.interop.SystemBinding;
import org.openlca.app.editors.sd.interop.VarBinding;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterRedef;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Vars;
import org.openlca.sd.xmile.Xmile;
import org.openlca.util.ParameterRedefSets;
import org.openlca.util.Strings;

class SdVarBindingDialog extends FormDialog {

	private final VarBinding binding;
	private final List<Var> vars;
	private final List<ParameterRedef> params;

	private TableViewer varsTable;
	private TableViewer paramsTable;
	private Text varsFilter;
	private Text paramsFilter;

	public static Optional<VarBinding> create(Xmile xmile, SystemBinding binding) {
		try {
			var vars = Vars.readFrom(xmile);
			if (vars.hasError()) {
				MsgBox.error("Failed to read variables from model", vars.error());
				return Optional.empty();
			}
			var params = ParameterRedefSets.allOf(Database.get(), binding.system())
					.parameters;
			var dialog = new SdVarBindingDialog(vars.value(), params);
			return dialog.open() == OK
					? Optional.of(dialog.binding)
					: Optional.empty();
		} catch (Exception e) {
			ErrorReporter.on("Failed to create binding dialog", e);
			return Optional.empty();
		}
	}

	private SdVarBindingDialog(List<Var> vars, List<ParameterRedef> params) {
		super(UI.shell());
		this.vars = vars;
		this.params = params;
		this.binding = new VarBinding();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Variable binding");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(800, 600);
	}

	@Override
	protected void createFormContent(IManagedForm mForm) {
		var tk = mForm.getToolkit();
		var body = UI.dialogBody(mForm.getForm(), tk);
		UI.gridLayout(body, 2, 10, 0);
		createVarsSection(body, tk);
		createParamsSection(body, tk);
	}

	private void createVarsSection(Composite body, FormToolkit tk) {
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);

		UI.label(comp, tk, "Model variable");

		varsFilter = UI.text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(varsFilter, true, false);
		varsFilter.setMessage("Search");

		varsTable = new TableViewer(comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		UI.gridData(varsTable.getControl(), true, true);
		varsTable.setContentProvider(ArrayContentProvider.getInstance());
		varsTable.setLabelProvider(new VarLabel());
		varsTable.setInput(vars);

		varsTable.addSelectionChangedListener(e -> {
			var selected = Viewers.getFirstSelected(varsTable);
			if (selected instanceof Var v) {
				binding.varId(v.name());
			}
		});

		// Handle filter
		varsFilter.addModifyListener(e -> {
			var filterText = varsFilter.getText().toLowerCase();
			if (Strings.nullOrEmpty(filterText)) {
				varsTable.setInput(vars);
			} else {
				var filtered = vars.stream()
						.filter(v -> v.name().label().toLowerCase().contains(filterText))
						.toList();
				varsTable.setInput(filtered);
			}
		});
	}

	private void createParamsSection(Composite body, FormToolkit tk) {
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);

		UI.label(comp, tk, "System parameter");

		paramsFilter = UI.text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(paramsFilter, true, false);
		paramsFilter.setMessage("Search");

		paramsTable = new TableViewer(comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		UI.gridData(paramsTable.getControl(), true, true);
		paramsTable.setContentProvider(ArrayContentProvider.getInstance());
		paramsTable.setLabelProvider(new ParamLabel());
		paramsTable.setInput(params);

		// Handle selection
		paramsTable.addSelectionChangedListener(e -> {
			var selected = Viewers.getFirstSelected(paramsTable);
			if (selected instanceof Parameter param) {
				binding.parameter(param);
			}
		});

		// Handle filter
		paramsFilter.addModifyListener(e -> {
			var filterText = paramsFilter.getText().toLowerCase();
			if (Strings.nullOrEmpty(filterText)) {
				paramsTable.setInput(params);
			} else {
				var filtered = params.stream()
						.filter(p -> p.name != null && p.name.toLowerCase().contains(filterText))
						.toList();
				paramsTable.setInput(filtered);
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

	private static class VarLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Var v))
				return null;
			return v.name().value();
		}
	}

	private static class ParamLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.FORMULA.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (obj instanceof ParameterRedef p) {
				return p.name;
			}
			return "";
		}
	}
}
