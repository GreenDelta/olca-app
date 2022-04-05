package org.openlca.app.db.tables;

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
import org.openlca.core.model.Process;
import org.openlca.core.model.ModelType;
import org.openlca.ilcd.models.Model;

import java.util.Collections;
import java.util.List;

public class ProcessTable extends SimpleFormEditor {

	private List<Process> processes;

	public static void show() {
		if (Database.get() == null) {
			MsgBox.info(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		var id = "DbProcessTable";
		Editors.open(new SimpleEditorInput(id, M.Parameters), id);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		try {
			processes = Database.get().getAll(Process.class);
		} catch (Exception e) {
			ErrorReporter.on("failed to load processes", e);
		}
		super.init(site, input);
	}

	@Override
	protected FormPage getPage() {
		return new Page(this);
	}

	private static class Page extends FormPage {

		private final List<Process> processes;

		Page(ProcessTable table) {
			super(table, "DbProcessTable", M.Processes);
			processes = table.processes != null
				? table.processes
				: Collections.emptyList();
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, M.Processes);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var filterComp = tk.createComposite(body);
			UI.gridLayout(filterComp, 2);
			UI.gridData(filterComp, true, false);
			var filter = UI.formText(filterComp, tk, M.Filter);

			var table = Tables.createViewer(body,
				M.Name,
				M.Category,
				M.FlowType,
				"Reference Flow",
				"ID");
			Tables.bindColumnWidths(table, 0.4, 0.1, 0.1, 0.2, 0.2);

			var label = new Label();
			table.setLabelProvider(label);
			Viewers.sortByLabels(table, label, 0, 1, 2, 3, 4);
			table.setInput(processes);
			TextFilter.on(table, filter);
			Actions.bind(table);
		}
	}

	private static class Label extends LabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof Process process))
				return null;
			return switch (col) {
				case 0 -> Images.get(ModelType.PROCESS);
				case 1 -> Images.get(process.category);
				case 2 -> Images.get(process.processType);
				case 3 -> Images.get(process.quantitativeReference.flow.flowType);
				default -> null;
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof Process process))
				return null;
			return switch (col) {
				case 0 -> Labels.name(process);
				case 1 -> process.category != null
					? process.category.toPath()
					: null;
				case 2 -> Labels.of(process.processType);
				case 3 -> Labels.name(process.quantitativeReference.flow);
				case 4 -> process.refId;
				default -> null;
			};
		}
	}

}
