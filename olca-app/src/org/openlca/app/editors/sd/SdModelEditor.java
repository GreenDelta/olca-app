package org.openlca.app.editors.sd;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.editors.Editors;
import org.openlca.app.util.ErrorReporter;

public class SdModelEditor extends FormEditor {

	public static final String ID = "editors.SdModelEditor";

	private File modelDir;
	private SdModelInfoPage infoPage;
	private SdModelParametersPage parametersPage;

	public static void open(File modelDir) {
		if (modelDir == null || !modelDir.exists() || !modelDir.isDirectory())
			return;
		var input = new SdModelEditorInput(modelDir);
		Editors.open(input, ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);

		if (input instanceof SdModelEditorInput sdInput) {
			this.modelDir = sdInput.getModelDir();
			setPartName(sdInput.getName());
		} else {
			throw new PartInitException("Invalid editor input: " + input);
		}
	}

	@Override
	protected void addPages() {
		try {
			infoPage = new SdModelInfoPage(this, modelDir);
			addPage(infoPage);

			parametersPage = new SdModelParametersPage(this, modelDir);
			addPage(parametersPage);
		} catch (Exception e) {
			ErrorReporter.on("Failed to create SD model editor pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (infoPage != null) {
			infoPage.doSave(monitor);
		}
		if (parametersPage != null) {
			parametersPage.doSave(monitor);
		}
		editorDirtyStateChanged();
	}

	@Override
	public void doSaveAs() {
		// Not supported
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	public File getModelDir() {
		return modelDir;
	}
}
