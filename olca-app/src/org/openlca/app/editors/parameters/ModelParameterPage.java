package org.openlca.app.editors.parameters;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.ModelPage;
import org.openlca.app.editors.lcia_methods.ImpactMethodEditor;
import org.openlca.app.editors.lcia_methods.ImpactMethodSourceHandler;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.app.util.tables.TableClipboard;
import org.openlca.app.util.tables.Tables;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.CategorizedEntity;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Process;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parameter page for LCIA methods or processes. */
public class ModelParameterPage<T extends CategorizedEntity> extends ModelPage<T> {

	public static final String ID = "ParameterPage";

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ParameterChangeSupport support;
	private ModelEditor<?> editor;
	private Supplier<List<Parameter>> supplier;
	private ParameterScope scope;
	private SourceHandler sourceHandler;

	private ModelParameterPage(ModelEditor<T> editor) {
		super(editor, ID, M.Parameters);
		this.editor = editor;
	}

	public static ModelParameterPage<Process> create(ProcessEditor editor) {
		ModelParameterPage<Process> page = new ModelParameterPage<>(editor);
		page.support = editor.getParameterSupport();
		page.supplier = () -> editor.getModel().getParameters();
		page.scope = ParameterScope.PROCESS;
		return page;
	}

	public static ModelParameterPage<ImpactMethod> create(ImpactMethodEditor editor) {
		ModelParameterPage<ImpactMethod> page = new ModelParameterPage<>(editor);
		page.support = editor.getParameterSupport();
		page.supplier = () -> editor.getModel().parameters;
		page.scope = ParameterScope.IMPACT_METHOD;
		page.sourceHandler = new ImpactMethodSourceHandler(editor);
		return page;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(this);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		try {
			createGlobalParamterSection(body);
			ParameterSection.forInputParameters(editor, support, body, toolkit,
					sourceHandler).setSupplier(supplier, scope);
			ParameterSection.forDependentParameters(editor, support, body,
					toolkit).setSupplier(supplier, scope);
			body.setFocus();
			form.reflow(true);
		} catch (Exception e) {
			log.error("failed to create parameter tables", e);
		}
	}

	private void createGlobalParamterSection(Composite body) {
		Section section = UI.section(body, toolkit, M.GlobalParameters);
		Composite client = UI.sectionClient(section, toolkit);
		UI.gridLayout(client, 1);
		String[] columns = { M.Name, M.Value,
				M.Uncertainty, M.Description };
		TableViewer table = Tables.createViewer(client, columns);
		ParameterLabel label = new ParameterLabel();
		table.setLabelProvider(label);
		Viewers.sortByLabels(table, label, 0, 2, 3);
		Viewers.sortByDouble(table, (Parameter p) -> p.getValue(), 1);
		Tables.bindColumnWidths(table.getTable(), 0.4, 0.3);
		table.getTable().getColumns()[1].setAlignment(SWT.RIGHT);
		section.setExpanded(false);
		setGlobalTableInput(table);
		bindGlobalParamActions(section, table);
	}

	private void bindGlobalParamActions(Section section, TableViewer table) {
		Action copy = TableClipboard.onCopy(table);
		Action refresh = Actions.create(M.Reload,
				Icon.REFRESH.descriptor(), () -> {
					setGlobalTableInput(table);
					support.evaluate();
					editor.setDirty(true);
				});
		Actions.bind(table, copy, refresh);
		Actions.bind(section, refresh);
	}

	private void setGlobalTableInput(TableViewer table) {
		IDatabase database = Database.get();
		ParameterDao dao = new ParameterDao(database);
		List<Parameter> params = dao.getGlobalParameters();
		Collections.sort(params, (p1, p2) -> {
			return Strings.compare(p1.getName(), p2.getName());
		});
		table.setInput(params);
	}

	private class ParameterLabel extends LabelProvider implements
			ITableLabelProvider {

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
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
				return Double.toString(parameter.getValue());
			case 2:
				return UncertaintyLabel.get(parameter.getUncertainty());
			case 3:
				return parameter.getDescription();
			default:
				return null;
			}
		}
	}

}
