package org.openlca.app.tools.libraries;

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
import org.openlca.app.rcp.Workspace;
import org.openlca.app.util.UI;
import org.openlca.core.database.IDatabase;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryInfo;

public class LibraryInfoPage extends SimpleFormEditor {

	private Library library;
	private LibraryInfo info;

	/**
	 * The database is only set if the library that is shown
	 * is linked to the currently activated database.
	 */
	private IDatabase db;

	public static void show(Library library) {
		if (library == null)
			return;
		var info = library.getInfo();
		var input = new SimpleEditorInput(
				library.id(), info.name() + " " + info.version());
		Editors.open(input, "LibraryInfoPage");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			var inp = (SimpleEditorInput) input;
			library = Workspace.getLibraryDir()
					.getLibrary(inp.id)
					.orElseThrow();
			info = library.getInfo();
			var db = Database.get();
			if (db != null && db.getLibraries().contains(library.id())) {
				this.db = db;
			}
		} catch (Exception e) {
			throw new PartInitException(
					"failed to init library page", e);
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

			var version = UI.formText(comp, tk, M.Version);
			version.setEditable(false);
			if (info.version() != null) {
				version.setText(info.version());
			}

			var description = UI.formMultiText(comp, tk, M.Description);
			description.setEditable(false);
			if (info.description() != null) {
				description.setText(info.description());
			}

			var isRegionalized = UI.formCheckBox(comp, tk, "Is regionalized");
			isRegionalized.setSelection(info.isRegionalized());
			isRegionalized.setEnabled(false);
		}
	}

}
