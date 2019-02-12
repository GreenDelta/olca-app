package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Colors;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Info;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.NativeSql;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.database.ProcessDao;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.expressions.FormulaInterpreter;
import org.openlca.expressions.Scope;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a simple editor that contains a table with all parameters of the
 * database (global and local).
 */
public class BigParameterTable extends SimpleFormEditor {

	public static void show() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		String id = "BigParameterTable";
		Editors.open(
				new SimpleEditorInput(id, id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		private final List<Param> params = new ArrayList<>();
		private TableViewer table;
		private Text filter;
		private FilterCombo filterCombo;

		public Page() {
			super(BigParameterTable.this,
					"BigParameterTable", M.Parameters);
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

			table = Tables.createViewer(
					body, M.Name, M.ParameterScope,
					M.Value, M.Formula, M.Description);
			double w = 1.0 / 5.0;
			Tables.bindColumnWidths(table, w, w, w, w, w);
			Label label = new Label();
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1, 3, 4);
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
					App.openEditor(p.parameter);
				} else if (p.owner != null) {
					App.openEditor(p.owner);
				}
			});
			Action onUsage = Actions.create(
					M.Usage, Icon.LINK.descriptor(), () -> {
						Param p = Viewers.getFirstSelected(table);
						if (p == null)
							return;
						ParameterUsagePage.show(p.parameter.name);
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
			Map<Long, ProcessDescriptor> processes = new ProcessDao(db)
					.getDescriptors().stream()
					.collect(Collectors.toMap(d -> d.id, d -> d));
			Map<Long, ImpactMethodDescriptor> methods = new ImpactMethodDao(db)
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
				} else if (pr.scope == ParameterScope.IMPACT_METHOD) {
					p.owner = methods.get(p.ownerID);
				}
			});

			Collections.sort(params);
		}

		private void evaluateFormulas() {
			FormulaInterpreter fi = buildInterpreter();
			for (Param param : params) {
				Parameter p = param.parameter;
				if (p.isInputParameter) {
					param.evalError = false;
					continue;
				}
				Scope scope = param.ownerID == null
						? fi.getGlobalScope()
						: fi.getScope(param.ownerID);
				try {
					p.value = scope.eval(p.formula);
					param.evalError = false;
				} catch (Exception e) {
					param.evalError = true;
				}
			}
		}

		/**
		 * Bind the parameter values and formulas to the respective scopes of a formula
		 * interpreter.
		 */
		private FormulaInterpreter buildInterpreter() {
			FormulaInterpreter fi = new FormulaInterpreter();
			for (Param param : params) {
				Scope scope = null;
				if (param.ownerID == null) {
					scope = fi.getGlobalScope();
				} else {
					scope = fi.getScope(param.ownerID);
					if (scope == null) {
						scope = fi.createScope(param.ownerID);
					}
				}
				Parameter p = param.parameter;
				if (p.isInputParameter) {
					scope.bind(p.name, Double.toString(p.value));
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

			// build dialog with validation
			InputDialog dialog = null;
			FormulaInterpreter fi = null;
			if (p.isInputParameter) {
				dialog = new InputDialog(UI.shell(),
						"#Edit value", "Set a new parameter value",
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
				Scope scope = param.ownerID == null
						? fi.getGlobalScope()
						: fi.getScope(param.ownerID);
				dialog = new InputDialog(UI.shell(),
						"#Edit formula", "Set a new parameter formula",
						p.formula, s -> {
							try {
								scope.eval(s);
								return null;
							} catch (Exception e) {
								return s + " " + M.IsInvalidFormula;
							}
						});
			}

			// parse the value from the dialog
			if (dialog.open() != Window.OK)
				return;
			String val = dialog.getValue();
			if (p.isInputParameter) {
				try {
					p.value = Double.parseDouble(val);
					param.evalError = false;
				} catch (Exception e) {
					param.evalError = true;
				}
			} else {
				try {
					p.formula = val;
					Scope scope = param.ownerID == null
							? fi.getGlobalScope()
							: fi.getScope(param.ownerID);
					p.value = scope.eval(val);
					param.evalError = false;
				} catch (Exception e) {
					param.evalError = true;
				}
			}

			// update the parameter in the database
			ParameterDao dao = new ParameterDao(
					Database.get());
			param.parameter = dao.update(p);
			table.refresh();
		}
	}

	/** Stores a parameter object and its owner. */
	private class Param implements Comparable<Param> {

		/**
		 * We have the owner ID as a separate field because a parameter could have a
		 * link to an owner that does not exist anymore in the database (it is an error
		 * but such things seem to happen).
		 */
		Long ownerID;

		/** If null, it is a global parameter. */
		CategorizedDescriptor owner;

		Parameter parameter;

		boolean evalError;

		@Override
		public int compareTo(Param other) {
			int c = Strings.compare(
					this.parameter.name,
					other.parameter.name);
			if (c != 0)
				return c;

			if (this.owner == null && other.owner == null)
				return 0;
			if (this.owner == null)
				return -1;
			if (other.owner == null)
				return 1;

			return Strings.compare(
					Labels.getDisplayName(this.owner),
					Labels.getDisplayName(other.owner));
		}

		boolean matches(String filter, int type) {
			if (type == FilterCombo.ERRORS)
				return evalError;
			if (parameter == null)
				return false;
			if (filter == null)
				return true;
			String f = filter.trim().toLowerCase();
			if (Strings.nullOrEmpty(f))
				return true;

			if (type == FilterCombo.ALL || type == FilterCombo.NAMES) {
				if (parameter.name != null) {
					String n = parameter.name.toLowerCase();
					if (n.contains(f))
						return true;
				}
			}

			if (type == FilterCombo.ALL || type == FilterCombo.SCOPES) {
				String scope = owner != null
						? Labels.getDisplayName(owner)
						: M.GlobalParameter;
				scope = scope == null ? "" : scope.toLowerCase();
				if (scope.contains(f))
					return true;
			}

			if (type == FilterCombo.ALL || type == FilterCombo.FORMULAS) {
				if (parameter.formula != null) {
					String formula = parameter.formula.toLowerCase();
					if (formula.contains(f)) {
						return true;
					}
				}
			}

			if (type == FilterCombo.ALL || type == FilterCombo.DESCRIPTIONS) {
				if (parameter.description != null) {
					String d = parameter.description.toLowerCase();
					if (d.contains(f)) {
						return true;
					}
				}
			}

			return false;
		}

		ParameterScope scope() {
			return parameter.scope == null
					? ParameterScope.GLOBAL
					: parameter.scope;
		}

	}

	private class Label extends LabelProvider
			implements ITableLabelProvider, ITableColorProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (col != 1 || !(obj instanceof Param))
				return null;
			Param p = (Param) obj;
			switch (p.scope()) {
			case GLOBAL:
				return Images.get(ModelType.PARAMETER);
			case IMPACT_METHOD:
				return Images.get(ModelType.IMPACT_METHOD);
			case PROCESS:
				return Images.get(ModelType.PROCESS);
			default:
				return null;
			}
		}

		@Override
		public Color getBackground(Object obj, int col) {
			return null;
		}

		@Override
		public Color getForeground(Object obj, int col) {
			if (!(obj instanceof Param))
				return null;
			Param param = (Param) obj;
			if (col == 1 &&
					param.scope() != ParameterScope.GLOBAL
					&& param.owner == null)
				return Colors.systemColor(SWT.COLOR_RED);
			if (param.evalError && (col == 2 || col == 3))
				return Colors.systemColor(SWT.COLOR_RED);
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Param))
				return null;
			Param param = (Param) obj;
			if (param.parameter == null)
				return " - ";
			Parameter p = param.parameter;
			switch (col) {
			case 0:
				return p.name;
			case 1:
				if (param.scope() == ParameterScope.GLOBAL)
					return M.GlobalParameter;
				if (param.owner == null)
					return "!! missing !!";
				return Labels.getDisplayName(param.owner);
			case 2:
				return Double.toString(p.value);
			case 3:
				if (p.isInputParameter)
					return null;
				return param.evalError
						? "!! error !! " + p.formula
						: p.formula;
			case 4:
				return p.description;
			default:
				return null;
			}
		}
	}

	private static class FilterCombo {

		static final int ALL = 0;
		static final int NAMES = 1;
		static final int SCOPES = 2;
		static final int FORMULAS = 3;
		static final int DESCRIPTIONS = 4;
		static final int ERRORS = 5;

		int type = ALL;
		Runnable onChange;

		static FilterCombo create(Composite comp, FormToolkit tk) {
			FilterCombo combo = new FilterCombo();
			Button button = tk.createButton(comp, "All columns", SWT.NONE);
			button.setImage(Icon.DOWN.get());
			Menu menu = new Menu(button);
			int[] types = {
					ALL,
					NAMES,
					SCOPES,
					FORMULAS,
					DESCRIPTIONS,
					ERRORS
			};
			for (int type : types) {
				MenuItem item = new MenuItem(menu, SWT.NONE);
				item.setText(label(type));
				Controls.onSelect(item, e -> {
					combo.type = type;
					button.setText(label(type));
					button.setToolTipText(label(type));
					button.pack();
					button.getParent().layout();
					if (combo.onChange != null) {
						combo.onChange.run();
					}
				});
			}
			button.setMenu(menu);
			Controls.onSelect(button, e -> menu.setVisible(true));
			return combo;
		}

		private static String label(int type) {
			switch (type) {
			case ALL:
				return "All columns";
			case NAMES:
				return "Names";
			case SCOPES:
				return "Parameter scopes";
			case FORMULAS:
				return "Formulas";
			case DESCRIPTIONS:
				return "Descriptions";
			case ERRORS:
				return "Evaluation errors";
			default:
				return "?";
			}
		}
	}
}
