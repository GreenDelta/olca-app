package org.openlca.app.editors.parameters;

import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia.ImpactCategoryEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.search.ParameterUsagePage;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.TableClipboard;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterizedEntity;
import org.openlca.core.model.Process;
import org.openlca.core.model.Uncertainty;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parameter page for LCIA methods or processes. */
public class ParameterPage<T extends ParameterizedEntity> extends ModelPage<T> {

	public static final String ID = "ParameterPage";
	private final Logger log = LoggerFactory.getLogger(getClass());

	final ModelEditor<T> editor;
	ParameterChangeSupport support;

	private TableViewer globalTable;
	Composite body;
	FormToolkit toolkit;

	private ParameterPage(ModelEditor<T> editor) {
		super(editor, ID, M.Parameters);
		this.editor = editor;
	}

	public static ParameterPage<Process> create(ProcessEditor editor) {
		var page = new ParameterPage<>(editor);
		page.support = editor.getParameterSupport();
		return page;
	}

	public static ParameterPage<ImpactCategory> create(ImpactCategoryEditor editor) {
		var page = new ParameterPage<>(editor);
		page.support = editor.getParameterSupport();
		return page;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		var form = UI.formHeader(this);
		toolkit = mform.getToolkit();
		body = UI.formBody(form, toolkit);
		try {
			createGlobalSection(body, toolkit);
			ParameterSection.forInputParameters(this);
			ParameterSection.forDependentParameters(this);
			body.setFocus();
			form.reflow(true);
		} catch (Exception e) {
			log.error("failed to create parameter tables", e);
		}
	}

	private void createGlobalSection(Composite body, FormToolkit tk) {
		var section = UI.section(body, tk, M.GlobalParameters);
		var comp = UI.sectionClient(section, tk);
		UI.gridLayout(comp, 1);
		String[] columns = { M.Name, M.Value,
				M.Uncertainty, M.Description };
		globalTable = Tables.createViewer(comp, columns);
		ParameterLabel label = new ParameterLabel();
		globalTable.setLabelProvider(label);
		Viewers.sortByLabels(globalTable, label, 0, 2, 3);
		Viewers.sortByDouble(globalTable, (Parameter p) -> p.value, 1);
		Tables.bindColumnWidths(globalTable.getTable(), 0.4, 0.3);
		globalTable.getTable().getColumns()[1].setAlignment(SWT.RIGHT);
		section.setExpanded(false);
		bindGlobalParamActions(section, globalTable);
		setGlobalTableInput();
	}

	private void bindGlobalParamActions(Section section, TableViewer table) {
		var copy = TableClipboard.onCopySelected(table);
		var refresh = Actions.create(M.Reload, Icon.REFRESH.descriptor(), () -> {
			setGlobalTableInput();
			support.evaluate();
			editor.setDirty(true);
		});
		var usage = Actions.create(M.Usage, Icon.LINK.descriptor(), () -> {
			Parameter p = Viewers.getFirstSelected(table);
			if (p != null) {
				ParameterUsagePage.show(p.name);
			}
		});
		Actions.bind(table, copy, refresh, usage);
		Actions.bind(section, refresh);
	}

	void setGlobalTableInput() {
		ParameterDao dao = new ParameterDao(Database.get());
		List<Parameter> params = dao.getGlobalParameters();
		params.sort((p1, p2) -> Strings.compare(p1.name, p2.name));
		globalTable.setInput(params);
	}

	private static class ParameterLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Parameter))
				return null;
			Parameter p = (Parameter) obj;
			switch (col) {
			case 0:
				return p.name;
			case 1:
				return Double.toString(p.value);
			case 2:
				return Uncertainty.string(p.uncertainty);
			case 3:
				return p.description;
			default:
				return null;
			}
		}
	}

}
