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
import org.openlca.core.model.Flow;
import org.openlca.core.model.ModelType;

public class FlowTable extends SimpleFormEditor {

	private List<Flow> flows;

	public static void show() {
		if (Database.get() == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var id = "DbFlowTable";
		Editors.open(new SimpleEditorInput(id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		try {
			flows = Database.get().getAll(Flow.class);
		} catch (Exception e) {
			ErrorReporter.on("failed to load flows", e);
		}
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final List<Flow> flows;

		Page(FlowTable table) {
			super(table, "DbFlowTable", M.Flows);
			flows = table.flows != null
				? table.flows
				: Collections.emptyList();
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, M.Flows);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var filterComp = tk.createComposite(body);
			UI.gridLayout(filterComp, 2);
			UI.gridData(filterComp, true, false);
			var filter = UI.formText(filterComp, tk, M.Filter);

			var table = Tables.createViewer(body,
				M.FlowType,
				M.Name,
				M.Category,
				M.ReferenceUnit,
				M.ReferenceFlowProperty,
				M.CASNumber,
				"Chemical formula",
				"ID");
			Tables.bindColumnWidths(table, 0.1, 0.2, 0.2, 0.1, 0.1, 0.1, 0.1, 0.1);

			var label = new FlowLabel();
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1, 2, 3, 4, 5, 6, 7);
			table.setInput(flows);
			TextFilter.on(table, filter);
			Actions.bind(table);
		}
	}

	private static class FlowLabel extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Flow flow))
				return null;
			return switch (col) {
				case 0 -> Images.get(flow.flowType);
				case 2 -> Images.get(flow.category);
				case 3 -> Images.get(ModelType.UNIT);
				case 4 -> Images.get(ModelType.FLOW_PROPERTY);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Flow flow))
				return null;
			return switch (col) {
				case 0 -> Labels.of(flow.flowType);
				case 1 -> Labels.name(flow);
				case 2 -> flow.category != null
					? flow.category.toPath()
					: null;
				case 3 -> Labels.name(flow.getReferenceUnit());
				case 4 -> Labels.name(flow.referenceFlowProperty);
				case 5 -> flow.casNumber;
				case 6 -> flow.formula;
				case 7 -> flow.refId;
				default -> null;
			};
		}
	}
}
