package org.openlca.app.wizards.io;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
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
				body, "Status", "Data set", "Message");
			table.setLabelProvider(new MessageLabel());
			Tables.bindColumnWidths(table, 0.2, 0.4, 0.4);
			table.setInput(log.messages());

			var onOpen = Actions.onOpen(() -> {
				ImportLog.Message message = Viewers.getFirstSelected(table);
				if (!message.hasDescriptor())
					return;
				App.open(message.descriptor());
			});
			Actions.bind(table, onOpen);
			Tables.onDoubleClick(table, $ -> onOpen.run());
		}

	}

	private static class MessageLabel extends BaseLabelProvider implements
		ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof ImportLog.Message message))
				return null;
			if (col == 0)
				return iconOf(message.state());
			if (col == 1 && message.hasDescriptor())
				return Images.get(message.descriptor());
			return null;
		}

		private Image iconOf(ImportLog.State state) {
			if (state == null)
				return null;
			return switch (state) {
				case IMPORTED, UPDATED -> Icon.IMPORT.get();
				case ERROR -> Icon.ERROR.get();
				case WARNING -> Icon.WARNING.get();
				case INFO -> Icon.INFO.get();
				case SKIPPED -> Icon.UNDO.get();
			};
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof ImportLog.Message message))
				return null;
			return switch (col) {
				case 0 -> labelOf(message.state());
				case 1 -> Labels.name(message.descriptor());
				case 2 -> message.message();
				default -> null;
			};
		}

		private String labelOf(ImportLog.State state) {
			if (state == null)
				return null;
			return switch (state) {
				case IMPORTED -> "Imported";
				case UPDATED -> "Updated";
				case ERROR -> "Error";
				case WARNING -> "Warning";
				case INFO -> "Information";
				case SKIPPED -> "Ignored";
			};
		}
	}
}
