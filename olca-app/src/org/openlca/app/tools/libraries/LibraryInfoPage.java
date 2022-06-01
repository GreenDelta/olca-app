package org.openlca.app.tools.libraries;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.openlca.app.M;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.Viewers;
import org.openlca.app.viewers.tables.Tables;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryInfo;

public class LibraryInfoPage extends SimpleFormEditor {

	private LibraryInfo info;

	public static void show(Library library) {
		if (library == null)
			return;
		var info = library.getInfo();
		var input = new SimpleEditorInput(
			library.name(), info.name());
		Editors.open(input, "LibraryInfoPage");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		super.init(site, input);
		try {
			var inp = (SimpleEditorInput) input;
			Library library = Workspace.getLibraryDir()
				.getLibrary(inp.id)
				.orElseThrow();
			info = library.getInfo();
		} catch (Exception e) {
			throw new PartInitException("failed to init library page", e);
		}
	}

	@Override
	protected FormPage getPage() {
		return new Page();
	}

	private class Page extends FormPage {

		Page() {
			super(LibraryInfoPage.this, "LibraryInfoPage", "Library");
		}

		@Override
		protected void createFormContent(IManagedForm mform) {
			var form = UI.formHeader(mform, M.Parameters);
			var tk = mform.getToolkit();
			var body = UI.formBody(form, tk);

			var comp = UI.formSection(body, tk, M.GeneralInformation);
			var name = UI.formText(comp, tk, M.Name);
			name.setEditable(false);
			if (info.name() != null) {
				name.setText(info.name());
			}

			var description = UI.formMultiText(comp, tk, M.Description);
			description.setEditable(false);
			if (info.description() != null) {
				description.setText(info.description());
			}

			var isRegionalized = UI.formCheckBox(comp, tk, "Is regionalized");
			isRegionalized.setSelection(info.isRegionalized());
			isRegionalized.setEnabled(false);

			if (!info.dependencies().isEmpty()) {
				renderDepTable(body, tk);
			}
		}

		private void renderDepTable(Composite body, FormToolkit tk) {
			var comp = UI.formSection(body, tk, "Library dependencies", 1);
			var table = Tables.createViewer(comp, "Library");
			Tables.bindColumnWidths(table, 1.0);
			table.setInput(info.dependencies());
			table.setLabelProvider(new DepLabel());

			var onOpen = Actions.onOpen(() -> {
				if (Viewers.getFirstSelected(table) instanceof String id) {
					var libDir = Workspace.getLibraryDir();
					libDir.getLibrary(id)
						.ifPresent(LibraryInfoPage::show);
				}
			});
			Actions.bind(table, onOpen);
			Tables.onDoubleClick(table, $ -> onOpen.run());
		}
	}

	private static class DepLabel extends BaseLabelProvider
		implements ITableLabelProvider {

		@Override
		public Image getColumnImage(Object obj, int col) {
			return Icon.LIBRARY.get();
		}

		@Override
		public String getColumnText(Object obj, int col) {
			return obj instanceof String s ? s : null;
		}
	}

}
