package org.openlca.app.editors.libraries;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryInfo;
import org.openlca.license.License;
import org.openlca.license.certificate.CertificateInfo;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.security.cert.CertificateException;


public class LibraryEditor extends FormEditor {

	public static String ID = "LibraryEditor";
	private static final String LICENSE_FILE = "license.json";

	public LibraryInfo info;
	public CertificateInfo license;

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
			Library library = Workspace.getLibraryDir()
					.getLibrary(inp.id)
					.orElseThrow();
			info = library.getInfo();
			license = getLicense(library.folder());

			setPartName(input.getName());
			setTitleImage(Icon.LIBRARY.get());
		} catch (Exception e) {
			throw new PartInitException("failed to init library page", e);
		}
	}

	public CertificateInfo getLicense(File folder) {
		if (info == null)
			return null;

		var file = new File(folder, LICENSE_FILE);
		if (!file.exists())
			return null;
		try {
			var reader = new JsonReader(new FileReader(file));
			var gson = new Gson();
			var mapType = new TypeToken<License>() {}.getType();
			License license = gson.fromJson(reader, mapType);

			var certBytes = license.certificate().getBytes();
			return CertificateInfo.of(new ByteArrayInputStream(certBytes));
		} catch (FileNotFoundException e) {
			var log = LoggerFactory.getLogger(getClass());
			log.error("failed to open the license.", e);
			return null;
		}
	}

	@Override
	protected void addPages() {
		try {
			addPage(new LibraryInfoPage(this));
			addPage(new LibraryLicensePage(this));
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
