package org.openlca.app.editors.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
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
import org.openlca.app.util.Info;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
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

		public Page() {
			super(BigParameterTable.this,
					"BigParameterTable", M.Parameters);
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, M.Parameters);
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);

			Composite filterComp = UI.formComposite(body, tk);
			UI.gridData(filterComp, true, false);
			Text filter = UI.formText(filterComp, tk, M.Filter);
			filter.addModifyListener(e -> {
				String t = filter.getText();
				if (Strings.nullOrEmpty(t)) {
					table.setInput(params);
				} else {
					List<Param> filtered = params.stream()
							.filter(p -> p.matches(t))
							.collect(Collectors.toList());
					table.setInput(filtered);
				}
			});

			table = Tables.createViewer(
					body, M.Name, "#Parameter scope",
					M.Value, M.Description);
			double w = 1.0 / 4.0;
			Tables.bindColumnWidths(table, w, w, w, w);
			table.setLabelProvider(new Label());
			// Actions: open, usage, edit value

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
						ParameterUsagePage.show(p.parameter.getName());
					});

			Actions.bind(table, onOpen, onUsage);
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

			new ParameterDao(db).getAll().forEach(p -> {
				Param param = new Param();
				param.parameter = p;
				params.add(param);
				if (p.scope == ParameterScope.GLOBAL)
					return;
				Long ownerId = owners.get(p.getId());
				if (ownerId == null)
					return;
				if (p.scope == ParameterScope.PROCESS) {
					param.owner = processes.get(ownerId);
				} else if (p.scope == ParameterScope.IMPACT_METHOD) {
					param.owner = methods.get(ownerId);
				}
			});

			Collections.sort(params);
		}

	}

	/** Stores a parameter object and its owner. */
	private class Param implements Comparable<Param> {
		/** If null, it is a global parameter. */
		CategorizedDescriptor owner;

		Parameter parameter;

		@Override
		public int compareTo(Param other) {
			int c = Strings.compare(
					this.parameter.getName(),
					other.parameter.getName());
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

		boolean matches(String filter) {
			if (filter == null)
				return true;
			if (parameter.getName() == null)
				return false;
			String f = filter.trim().toLowerCase();
			String n = parameter.getName().toLowerCase();
			return n.contains(f);
		}

		ParameterScope scope() {
			return parameter.scope == null
					? ParameterScope.GLOBAL
					: parameter.scope;
		}

	}

	private class Label extends LabelProvider implements ITableLabelProvider {

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
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Param))
				return null;
			Param param = (Param) obj;
			if (param.parameter == null)
				return " - ";
			Parameter p = param.parameter;
			switch (col) {
			case 0:
				return p.getName();
			case 1:
				if (param.scope() == ParameterScope.GLOBAL)
					return M.GlobalParameter;
				if (param.owner == null)
					return "!! missing !!";
				return Labels.getDisplayName(param.owner);
			case 2:
				return p.isInputParameter
						? Double.toString(p.value)
						: p.formula + " = " + Numbers.format(p.value);
			case 3:
				return p.getDescription();
			default:
				return null;
			}
		}

	}

}
