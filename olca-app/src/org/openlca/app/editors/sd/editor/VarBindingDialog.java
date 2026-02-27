package org.openlca.app.editors.sd.editor;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

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
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.commons.Strings;
import org.openlca.core.model.ParameterRedef;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.sd.model.EntityRef;
import org.openlca.sd.model.Id;
import org.openlca.sd.model.SystemBinding;
import org.openlca.sd.model.Var;
import org.openlca.sd.model.VarBinding;
import org.openlca.util.ParameterRedefSets;

class VarBindingDialog extends FormDialog {

	private Id selectedVarId;
	private ParameterRedef selectedParam;
	private VarBinding result;

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
					? Optional.ofNullable(dialog.result)
					: Optional.empty();
		} catch (Exception e) {
			ErrorReporter.on("Failed to create binding dialog", e);
			return Optional.empty();
		}
	}

	private static List<ParameterRedef> getFreeParamsOf(SystemBinding binding) {
		var db = Database.get();
		var sysRef = binding.system();
		if (sysRef == null || db == null)
			return List.of();
		var system = db.get(ProductSystem.class, sysRef.refId());
		if (system == null)
			return List.of();

		var all = ParameterRedefSets.allOf(
				db, Descriptor.of(system)).parameters;

		Function<ParameterRedef, String> keyFn = p -> {
			if (Strings.isBlank(p.name))
				return "--";
			var name = p.name.strip().toLowerCase();
			return p.contextId == null
					? name
					: name + "//" + p.contextId;
		};

		// build bound set from existing VarBindings
		var bound = new HashSet<String>();
		for (var vb : binding.varBindings()) {
			if (Strings.isBlank(vb.parameter()))
				continue;
			var name = vb.parameter().strip().toLowerCase();
			if (vb.context() == null) {
				bound.add(name);
			} else {
				var ctx = db.get(
						vb.context().type().getModelClass(),
						vb.context().refId());
				bound.add(ctx != null
						? name + "//" + ctx.id
						: name);
			}
		}

		return all.stream()
			.filter(p -> !bound.contains(keyFn.apply(p)))
			.sorted((pi, pj) -> Strings.compareIgnoreCase(pi.name, pj.name))
			.toList();
	}

	private VarBindingDialog(List<Var> vars, List<ParameterRedef> params) {
		super(UI.shell());
		this.vars = vars;
		this.params = params;
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
				selectedVarId = v.name();
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

		// remove the selection if it does not match the filter
		if (selectedVarId != null && !matcher.test(selectedVarId)) {
			selectedVarId = null;
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
				selectedParam = param;
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

		// remove the selection if it does not match the filter
		if (selectedParam != null && !matcher.test(selectedParam)) {
			selectedParam = null;
			checkOk();
		}

		var filtered = params.stream()
				.filter(matcher)
				.toList();
		paramsTable.setInput(filtered);
	}

	private void checkOk() {
		var ok = selectedVarId != null && selectedParam != null;
		var btn = getButton(IDialogConstants.OK_ID);
		if (btn != null) {
			btn.setEnabled(ok);
		}
	}

	@Override
	protected void okPressed() {
		if (selectedVarId == null || selectedParam == null)
			return;

		// resolve context from the selected ParameterRedef
		EntityRef context = null;
		if (selectedParam.contextId != null
				&& selectedParam.contextType != null) {
			var db = Database.get();
			if (db != null) {
				var ctx = db.getDescriptor(
						selectedParam.contextType.getModelClass(),
						selectedParam.contextId);
				if (ctx != null) {
					context = EntityRef.of(ctx);
				}
			}
		}

		result = new VarBinding(selectedVarId, selectedParam.name, context);
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
