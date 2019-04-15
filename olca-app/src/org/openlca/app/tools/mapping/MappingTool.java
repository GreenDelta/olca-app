package org.openlca.app.tools.mapping;

import java.util.UUID;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.util.Info;
import org.openlca.app.util.UI;
import org.openlca.app.util.tables.Tables;

public class MappingTool extends SimpleFormEditor {

	public static void open() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		Editors.open(new SimpleEditorInput(
				"FlowMappings", UUID.randomUUID().toString(), "#Flow mapping"),
				"MappingTool");
	}

	@Override
	protected FormPage getPage() {
		return new MappingPage();
	}

	private class MappingPage extends FormPage {

		public MappingPage() {
			super(MappingTool.this, "MappingPage", "#Flow mapping");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			ScrolledForm form = UI.formHeader(mform, "#Flow mapping");
			FormToolkit tk = mform.getToolkit();
			Composite body = UI.formBody(form, tk);

			TableViewer table = Tables.createViewer(
					body,
					"Source flow",
					"Source category",
					"Source unit",
					"Target flow",
					"Target category",
					"Target unit",
					"Conversion factor",
					"Default provider");
			table.setLabelProvider(new LabelProvider());

			double w = 1.0 / 8.0;
			Tables.bindColumnWidths(table, w, w, w, w, w, w, w, w);

			form.reflow(true);
		}
	}
}
