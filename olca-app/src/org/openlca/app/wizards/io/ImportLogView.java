package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.io.ImportLog;

public class ImportLogView extends SimpleFormEditor {

	private ImportLog log;

	public static void open(ImportLog log) {
		var id = Cache.getAppCache().put(log);
		var input = new SimpleEditorInput(id, "Import details");
		Editors.open(input, "ImportLogView");
	}

	@Override
	public void init(IEditorSite site, IEditorInput raw)
		throws PartInitException {
		super.init(site, raw);
		if (!(raw instanceof SimpleEditorInput input))
			return;
		log = Cache.getAppCache().remove(input.id);
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		Page() {
			super(ImportLogView.this, "ImportLogView.Page", "Import details");
		}

		@Override
		protected void createFormContent(IManagedForm mForm) {
			var form = UI.formHeader(mForm, "Import details", Icon.IMPORT.get());
			var tk = mForm.getToolkit();
			var body = UI.formBody(form, tk);
			var table = Tables.createViewer(
				body, "Status", "Message", "Data set");
			table.setLabelProvider(new MessageLabel());
			table.setInput(log.messages());
		}

	}

	private static class MessageLabel extends BaseLabelProvider implements
		ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImportLog.Message message))
				return null;
			return switch (col) {
				case 0 -> message.state() != null
					? message.state().name()
					: "";
				case 1 -> message.message();
				case 2 -> Labels.name(message.descriptor());
				default -> null;
			};
		}
	}
}
