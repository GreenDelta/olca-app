package org.openlca.app.db.tables;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Labels;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.ModelType;
import org.openlca.util.Categories;

public class FlowPropertyTable extends SimpleFormEditor {

	private List<FlowProperty> properties;

	public static void show() {
		if (Database.get() == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var id = "DbFlowPropertyTable";
		Editors.open(new SimpleEditorInput(id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		try {
			properties = Database.get().getAll(FlowProperty.class);
		} catch (Exception e) {
			ErrorReporter.on("failed to load flows properties", e);
		}
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final List<FlowProperty> properties;

		Page(FlowPropertyTable table) {
			super(table, "DbFlowPropertyTable", M.FlowProperties);
			properties = table.properties != null
				? table.properties
				: Collections.emptyList();
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, M.FlowProperties);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var filterComp = tk.createComposite(body);
			UI.gridLayout(filterComp, 2);
			UI.gridData(filterComp, true, false);
			var filter = UI.formText(filterComp, tk, M.Filter);

			var table = Tables.createViewer(body,
				M.FlowPropertyType,
				M.Name,
				M.Category,
				M.ReferenceUnit,
				"ID");
			Tables.bindColumnWidths(table, 0.2, 0.2, 0.2, 0.2, 0.2);

			var label = new Label(Database.get());
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1, 2, 3, 4);
			table.setInput(properties);
			TextFilter.on(table, filter);
			Actions.bind(table);
		}
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		private final Categories.PathBuilder categories;

		Label(IDatabase db) {
			this.categories = Categories.pathsOf(db);
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof FlowProperty property))
				return null;
			return switch (col) {
				case 0 -> Images.get(ModelType.FLOW_PROPERTY);
				case 2 -> Images.get(property.category);
				case 3 -> Images.get(ModelType.UNIT);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof FlowProperty property))
				return null;
			return switch (col) {
				case 0 -> Labels.of(property.flowPropertyType);
				case 1 -> Labels.name(property);
				case 2 -> property.category != null
					? categories.pathOf(property.category.id)
					: null;
				case 3 -> Labels.name(property.getReferenceUnit());
				case 4 -> property.refId;
				default -> null;
			};
		}
	}

}
