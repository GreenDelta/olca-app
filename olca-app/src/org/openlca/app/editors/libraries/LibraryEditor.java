package org.openlca.app.editors.libraries;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Libraries;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryInfo;


public class LibraryEditor extends FormEditor {

	public static String ID = "LibraryEditor";

	public Library library;
	public LibraryInfo info;

	public static void open(Library library) {
		if (library == null)
			return;
		var info = library.getInfo();
		var input = new SimpleEditorInput(library.name(), info.name());
		Editors.open(input, "editors.LibraryEditor");
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

			setPartName(input.getName());
			setTitleImage(Icon.LIBRARY.get());
		} catch (Exception e) {
			throw new PartInitException("failed to init library page", e);
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new LibraryInfoPage(this));
			var license = Libraries.getLicense(library.folder());
			if (license.isPresent()) {
				addPage(new LibraryLicensePage(this));
			}
		} catch (Exception e) {
			ErrorReporter.on("failed to add page", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
