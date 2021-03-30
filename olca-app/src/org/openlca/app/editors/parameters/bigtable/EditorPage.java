package org.openlca.app.editors.parameters.bigtable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
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
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.core.model.Version;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EditorPage extends FormPage {

	private final List<Param> params = new ArrayList<>();
	private TableViewer table;
	private Text filter;
	private FilterCombo filterCombo;

	public EditorPage(BigParameterTable table) {
		super(table,"BigParameterTable", M.Parameters);
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
				this::initParams,
				() -> table.setInput(params));
	}

	private void bindActions() {
		Action onOpen = Actions.onOpen(() -> {
			Param p = Viewers.getFirstSelected(table);
			if (p == null)
				return;
			if (p.scope() == ParameterScope.GLOBAL) {
				App.open(p.parameter);
			} else if (p.owner != null) {
				App.open(p.owner);
			}
		});
		Action onUsage = Actions.create(
				M.Usage, Icon.LINK.descriptor(), () -> {
					Param p = Viewers.getFirstSelected(table);
					if (p == null)
						return;
					ParameterUsagePage.show(p.parameter, p.owner);
				});
		Action onEvaluate = Actions.create(
				M.EvaluateAllFormulas, Icon.RUN.descriptor(), () -> {
					App.runWithProgress(M.EvaluateAllFormulas,
							this::evaluateFormulas, () -> {
								table.setInput(params);
								filter.setText("");
							});
				});
		Action onEdit = Actions.create(M.Edit,
				Icon.EDIT.descriptor(), this::onEdit);

		Actions.bind(table, onOpen, onUsage, onEvaluate, onEdit);
		Tables.onDoubleClick(table, e -> onOpen.run());
	}

	private void initParams() {
		IDatabase db = Database.get();
		var processes = new ProcessDao(db)
				.getDescriptors().stream()
				.collect(Collectors.toMap(d -> d.id, d -> d));
		var impacts = new ImpactCategoryDao(db)
				.getDescriptors().stream()
				.collect(Collectors.toMap(d -> d.id, d -> d));
		Map<Long, Long> owners = new HashMap<>();
		try {
			String sql = "select id, f_owner from tbl_parameters";
			NativeSql.on(db).query(sql, r -> {
				owners.put(r.getLong(1), r.getLong(2));
				return true;
			});
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to query parameter onwers", e);
		}

		new ParameterDao(db).getAll().forEach(pr -> {
			Param p = new Param();
			p.parameter = pr;
			params.add(p);
			if (pr.scope == ParameterScope.GLOBAL)
				return;
			p.ownerID = owners.get(pr.id);
			if (p.ownerID == null)
				return;
			if (pr.scope == ParameterScope.PROCESS) {
				p.owner = processes.get(p.ownerID);
			} else if (pr.scope == ParameterScope.IMPACT) {
				p.owner = impacts.get(p.ownerID);
			}
		});

		Collections.sort(params);
	}

	private void evaluateFormulas() {
		var fi = buildInterpreter();
		for (var param : params) {
			var p = param.parameter;
			if (p.isInputParameter) {
				param.evalError = false;
				continue;
			}
			var scope = param.ownerID == null
					? fi.getGlobalScope()
					: fi.getScopeOrGlobal(param.ownerID);
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
	private FormulaInterpreter buildInterpreter() {
		var fi = new FormulaInterpreter();
		for (var param : params) {
			Scope scope = param.ownerID == null
					? fi.getGlobalScope()
					: fi.getOrCreate(param.ownerID);
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
		Parameter p = param.parameter;

		// first check that the parameter or the owner
		// is currently not edited in another editor
		if (param.owner == null && App.hasDirtyEditor(p)) {
			MsgBox.info("Cannot edit " + p.name,
					"The parameter is currently "
							+ "modified in another editor.");
			return;
		}
		if (param.owner != null
				&& App.hasDirtyEditor(param.owner)) {
			String label = Strings.cut(
					Labels.name(param.owner), 50);
			MsgBox.info("Cannot edit " + p.name, label +
					" is currently modified in another editor.");
			return;
		}

		// build dialog with validation
		InputDialog dialog;
		FormulaInterpreter fi = null;
		if (p.isInputParameter) {
			dialog = new InputDialog(UI.shell(),
					"Edit value", "Set a new parameter value",
					Double.toString(p.value), s -> {
						try {
							Double.parseDouble(s);
							return null;
						} catch (Exception e) {
							return s + " " + M.IsNotValidNumber;
						}
					});
		} else {
			fi = buildInterpreter();
			var scope = param.ownerID == null
					? fi.getGlobalScope()
					: fi.getScopeOrGlobal(param.ownerID);
			dialog = new InputDialog(UI.shell(),
					"Edit formula", "Set a new parameter formula",
					p.formula, s -> {
						try {
							scope.eval(s);
							return null;
						} catch (Exception e) {
							return s + " " + M.IsInvalidFormula;
						}
					});
		}

		// sync the parameter
		if (dialog.open() != Window.OK)
			return;
		IDatabase db = Database.get();
		ParameterDao dao = new ParameterDao(db);
		p = dao.getForId(p.id);

		String val = dialog.getValue();
		if (p.isInputParameter) {
			try {
				p.value = Double.parseDouble(val);
				param.evalError = false;
			} catch (Exception e) {
				param.evalError = true;
			}
		} else if (fi != null) {
			try {
				p.formula = val;
				var scope = param.ownerID == null
						? fi.getGlobalScope()
						: fi.getScopeOrGlobal(param.ownerID);
				p.value = scope.eval(val);
				param.evalError = false;
			} catch (Exception e) {
				param.evalError = true;
			}
		}

		// update the parameter in the database
		long time = Calendar.getInstance()
				.getTimeInMillis();
		p.lastChange = time;
		Version.incUpdate(p);
		param.parameter = dao.update(p);

		// update the owner; we also close a possible
		// opened editor just to make sure that the
		// user does not get confused with a state that
		// is not in sync with the database
		if (param.owner == null) {
			App.close(p);
		} else {
			if (param.owner.type == ModelType.PROCESS) {
				ProcessDao pdao = new ProcessDao(db);
				Process proc = pdao.getForId(param.owner.id);
				if (proc != null) {
					Version.incUpdate(proc);
					proc.lastChange = time;
				}
				proc = pdao.update(proc);
				App.close(proc);
			} else if (param.owner.type == ModelType.IMPACT_METHOD) {
				ImpactMethodDao mdao = new ImpactMethodDao(db);
				ImpactMethod method = mdao.getForId(param.owner.id);
				if (method != null) {
					Version.incUpdate(method);
					method.lastChange = time;
				}
				method = mdao.update(method);
				App.close(method);
			}
		}

		table.refresh();
	}
}