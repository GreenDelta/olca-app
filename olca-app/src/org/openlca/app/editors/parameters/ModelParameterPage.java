package org.openlca.app.editors.parameters;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.editors.ModelEditor;
import org.openlca.app.editors.lcia_methods.ImpactMethodEditor;
import org.openlca.app.editors.processes.ProcessEditor;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Actions;
import org.openlca.app.util.TableClipboard;
import org.openlca.app.util.Tables;
import org.openlca.app.util.UI;
import org.openlca.app.util.UncertaintyLabel;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ParameterDao;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parameter page for LCIA methods or processes. */
public class ModelParameterPage extends FormPage {

	public static final String ID = "ParameterPage";

	private Logger log = LoggerFactory.getLogger(getClass());
	private FormToolkit toolkit;
	private ParameterChangeSupport support;
	private ModelEditor<?> editor;
	private Supplier<List<Parameter>> supplier;
	private ParameterScope scope;

	public ModelParameterPage(ProcessEditor editor) {
		super(editor, ID, Messages.Parameters);
		this.support = editor.getParameterSupport();
		this.editor = editor;
		this.supplier = () -> editor.getModel().getParameters();
		this.scope = ParameterScope.PROCESS;
	}

	public ModelParameterPage(ImpactMethodEditor editor) {
		super(editor, ID, Messages.Parameters);
		this.support = editor.getParameterSupport();
		this.editor = editor;
		this.supplier = () -> editor.getModel().getParameters();
		this.scope = ParameterScope.IMPACT_METHOD;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = UI.formHeader(managedForm, Messages.Parameters);
		toolkit = managedForm.getToolkit();
		Composite body = UI.formBody(form, toolkit);
		try {
			createGlobalParamterSection(body);
			ParameterSection
					.forInputParameters(editor, support, body, toolkit)
					.setSupplier(supplier, scope);
			ParameterSection
					.forDependentParameters(editor, support, body, toolkit)
					.setSupplier(supplier, scope);
			body.setFocus();
			form.reflow(true);
		} catch (Exception e) {
			log.error("failed to create parameter tables", e);
		}
	}

	private void createGlobalParamterSection(Composite body) {
		Section section = UI.section(body, toolkit, Messages.GlobalParameters);
		Composite client = UI.sectionClient(section, toolkit);
		UI.gridLayout(client, 1);
		String[] columns = { Messages.Name, Messages.Value,
				Messages.Uncertainty, Messages.Description };
		TableViewer table = Tables.createViewer(client, columns);
		table.setLabelProvider(new ParameterLabel());
		Tables.bindColumnWidths(table.getTable(), 0.4, 0.3);
		section.setExpanded(false);
		setGlobalTableInput(table);
		bindGlobalParamActions(section, table);
	}

	private void bindGlobalParamActions(Section section, TableViewer table) {
		Action copy = TableClipboard.onCopy(table);
		Action refresh = Actions.create(Messages.Reload,
				ImageType.REFRESH_ICON.getDescriptor(),
				() -> {
					setGlobalTableInput(table);
					support.evaluate();
					editor.setDirty(true);
				});
		Action edit = Actions.create(Messages.Edit,
				ImageType.EDIT_16.getDescriptor(),
				GlobalParameterEditor::open);
		Actions.bind(table, copy, refresh, edit);
		Actions.bind(section, refresh, edit);
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
