package org.openlca.app.editors.sd.editor;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.interop.SystemBinding;
import org.openlca.app.editors.sd.interop.VarBinding;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.commons.Strings;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.sd.eqn.Id;
import org.openlca.sd.eqn.Var;
import org.openlca.util.ParameterRedefSets;

class VarBindingDialog extends FormDialog {

	private final VarBinding binding;
	private final List<Var> vars;
	private final List<ParameterRedef> params;

	private TableViewer varsTable;
	private TableViewer paramsTable;

	public static Optional<VarBinding> create(
			List<Var> vars, SystemBinding binding
	) {
		try {
			var params = getFreeParamsOf(binding);
			var dialog = new VarBindingDialog(vars, params);
			return dialog.open() == OK
					? Optional.of(dialog.binding)
					: Optional.empty();
		} catch (Exception e) {
			ErrorReporter.on("Failed to create binding dialog", e);
			return Optional.empty();
		}
	}

	private static List<ParameterRedef> getFreeParamsOf(SystemBinding binding) {
		var all = ParameterRedefSets.allOf(
			Database.get(), Descriptor.of(binding.system())).parameters;

		Function<ParameterRedef, String> keyFn = p -> {
			if (Strings.isBlank(p.name))
				return "--";
			var name = p.name.strip().toLowerCase();
			return p.contextId == null
					? name
					: name + "//" + p.contextId;
		};

		var bound = binding.varBindings().stream()
				.map(VarBinding::parameter)
				.map(keyFn)
				.collect(Collectors.toSet());

		return all.stream()
			.filter(p -> !bound.contains(keyFn.apply(p)))
			.sorted((pi, pj) -> Strings.compareIgnoreCase(pi.name, pj.name))
			.toList();
	}

	private VarBindingDialog(List<Var> vars, List<ParameterRedef> params) {
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
		checkOk();
	}

	private void createVarsSection(Composite body, FormToolkit tk) {
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);

		UI.label(comp, tk, "Model variable");

		var varsFilter = UI.text(comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(varsFilter, true, false);
		varsFilter.setMessage("Search");
		varsFilter.addModifyListener(e -> filterVars(varsFilter.getText()));

		varsTable = new TableViewer(comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		UI.gridData(varsTable.getControl(), true, true);
		varsTable.setContentProvider(ArrayContentProvider.getInstance());
		varsTable.setLabelProvider(new VarLabel());
		varsTable.setInput(vars);

		varsTable.addSelectionChangedListener(e -> {
			var selected = Viewers.getFirstSelected(varsTable);
			if (selected instanceof Var v) {
				binding.varId(v.name());
				checkOk();
			}
		});
	}

	private void filterVars(String text) {
		if (Strings.isBlank(text)) {
			varsTable.setInput(vars);
			return;
		}
		var f = text.strip().toLowerCase();
		Predicate<Id> matcher = (id) ->
				id != null && id.label().toLowerCase().contains(f);

		// remove the binding, if it does not match the filter
		if (binding.varId() != null && !matcher.test(binding.varId())) {
				binding.varId(null);
				checkOk();
		}

		var filtered = vars.stream()
				.filter(v -> matcher.test(v.name()))
				.toList();
		varsTable.setInput(filtered);
	}

	private void createParamsSection(Composite body, FormToolkit tk) {
		var comp = tk.createComposite(body);
		UI.gridData(comp, true, true);
		UI.gridLayout(comp, 1);

		UI.label(comp, tk, "System parameter");

		var paramsFilter = UI.text(
				comp, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		UI.gridData(paramsFilter, true, false);
		paramsFilter.setMessage("Search");
		paramsFilter.addModifyListener(
				e -> filterParameters(paramsFilter.getText()));

		paramsTable = new TableViewer(
				comp, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		UI.gridData(paramsTable.getControl(), true, true);
		paramsTable.setContentProvider(ArrayContentProvider.getInstance());
		paramsTable.setLabelProvider(new ParamLabel());
		paramsTable.setInput(params);

		paramsTable.addSelectionChangedListener(e -> {
			var selected = Viewers.getFirstSelected(paramsTable);
			if (selected instanceof ParameterRedef param) {
				binding.parameter(param);
				checkOk();
			}
		});
	}

	private void filterParameters(String text) {
		if (Strings.isBlank(text)) {
			paramsTable.setInput(params);
			return;
		}
		var f = text.strip().toLowerCase();
		Predicate<ParameterRedef> matcher = (p) -> !Strings.isBlank(p.name)
			&& p.name.toLowerCase().contains(f);

		// remove the binding, if it does not match the filter
		if (binding.parameter() != null && !matcher.test(binding.parameter())) {
			binding.parameter(null);
			checkOk();
		}

		var filtered = params.stream()
				.filter(matcher)
				.toList();
		paramsTable.setInput(filtered);
	}

	private void checkOk() {
		var ok = binding.varId() != null && binding.parameter() != null;
		var btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) {
			btn.setEnabled(ok);
		}
	}

	@Override
	protected void okPressed() {
		if (binding.varId() == null || binding.parameter() == null)
			return;
		super.okPressed();
	}

	private static class VarLabel extends BaseLabelProvider
			implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.FORMULA.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Var v))
				return null;
			return v.name().label();
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
