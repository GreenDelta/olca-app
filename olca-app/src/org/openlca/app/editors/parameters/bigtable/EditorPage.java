package org.openlca.app.editors.parameters.bigtable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.model.ParameterScope;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.util.Strings;

class EditorPage extends FormPage {

	private final List<Param> params = new ArrayList<>();
	private TableViewer table;
	private Text filter;
	private FilterCombo filterCombo;

	public EditorPage(BigParameterTable table) {
		super(table, "BigParameterTable", M.Parameters);
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		ScrolledForm form = UI.formHeader(mform, M.Parameters);
		FormToolkit tk = mform.getToolkit();
		Composite body = UI.formBody(form, tk);

		Composite filterComp = tk.createComposite(body);
		UI.gridLayout(filterComp, 3);
		UI.gridData(filterComp, true, false);
		filter = UI.formText(filterComp, tk, M.Filter);
		filterCombo = FilterCombo.create(filterComp, tk);

		Runnable doFilter = () -> {
			String t = filter.getText();
			if (Strings.nullOrEmpty(t)
				&& filterCombo.type != FilterCombo.ERRORS) {
				table.setInput(params);
			} else {
				List<Param> filtered = params.stream()
					.filter(p -> p.matches(t, filterCombo.type))
					.collect(Collectors.toList());
				table.setInput(filtered);
			}
		};
		filter.addModifyListener(e -> doFilter.run());
		filterCombo.onChange = doFilter;

		table = Tables.createViewer(body,
			M.Name,
			M.ParameterScope,
			M.Value,
			M.Formula,
			M.Uncertainty,
			M.Description);
		double w = 1.0 / 6.0;
		Tables.bindColumnWidths(table, w, w, w, w, w, w);
		Label label = new Label();
		table.setLabelProvider(label);
		Viewers.sortByLabels(table, label, 0, 1, 3, 4, 5);
		Viewers.sortByDouble(table, (Param p) -> p.parameter.value, 2);

		bindActions();
		mform.reflow(true);
		App.runWithProgress(
			"Loading parameters ...",
			() -> Param.fetchAll(Database.get(), params),
			() -> table.setInput(params));
	}

	private void bindActions() {

		var onOpen = Actions.onOpen(() -> {
			Param p = Viewers.getFirstSelected(table);
			if (p == null)
				return;
			if (p.scope() == ParameterScope.GLOBAL) {
				App.open(p.parameter);
			} else if (p.owner != null) {
				App.open(p.owner);
			}
		});

		var onUsage = Actions.create(
			M.Usage, Icon.LINK.descriptor(), () -> {
				Param p = Viewers.getFirstSelected(table);
				if (p == null)
					return;
				ParameterUsagePage.show(p.parameter, p.owner);
			});

		var onEvaluate = Actions.create(
			M.EvaluateAllFormulas, Icon.RUN.descriptor(), () -> App.runWithProgress(
				M.EvaluateAllFormulas, this::evaluateFormulas, () -> {
					table.setInput(params);
					filter.setText("");
				}));

		var onEdit = Actions.create(M.Edit, Icon.EDIT.descriptor(), this::onEdit);
		var onCopy = TableClipboard.onCopySelected(table);

		Actions.bind(table, onOpen, onUsage, onEvaluate, onEdit, onCopy);

		Tables.onDoubleClick(table, e -> {
			var cell = table.getCell(new Point(e.x, e.y));
			if (cell == null)
				return;
			switch (cell.getColumnIndex()) {
				case 0, 1 -> onOpen.run();
				case 2, 3, 4 -> onEdit.run();
			}
		});
	}

	private void evaluateFormulas() {
		var fi = buildInterpreter();
		for (var param : params) {
			var p = param.parameter;
			if (p.isInputParameter) {
				param.evalError = false;
				continue;
			}
			var scope = param.isGlobal()
				? fi.getGlobalScope()
				: fi.getScopeOrGlobal(param.ownerId());
			try {
				p.value = scope.eval(p.formula);
				param.evalError = false;
			} catch (Exception e) {
				param.evalError = true;
			}
		}
	}

	/**
	 * Bind the parameter values and formulas to the respective scopes of a
	 * formula interpreter.
	 */
	FormulaInterpreter buildInterpreter() {
		var fi = new FormulaInterpreter();
		for (var param : params) {
			Scope scope = param.isGlobal()
				? fi.getGlobalScope()
				: fi.getOrCreate(param.ownerId());
			var p = param.parameter;
			if (p.isInputParameter) {
				scope.bind(p.name, p.value);
			} else {
				scope.bind(p.name, p.formula);
			}
		}
		return fi;
	}

	private void onEdit() {
		Param param = Viewers.getFirstSelected(table);
		if (param == null || param.parameter == null)
			return;
		if (ValueEditor.edit(this, param)) {
			table.refresh();
		}
	}
}
