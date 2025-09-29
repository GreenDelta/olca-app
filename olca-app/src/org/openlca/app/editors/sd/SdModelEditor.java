package org.openlca.app.editors.sd;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.sd.xmile.Xmile;

public class SdModelEditor extends FormEditor {

	public static final String ID = "editors.SdModelEditor";

	private File modelDir;
	private Xmile xmile;

	public static void open(File modelDir) {
		if (modelDir == null || !modelDir.exists() || !modelDir.isDirectory())
			return;
		var xmile = SystemDynamics.openModel(modelDir);
		if (xmile.hasError()) {
			MsgBox.error("Failed to read model",
					"Failed to read the model from the model folder: " + xmile.error());
			return;
		}
		var key = AppContext.put(xmile.value());
		Editors.open(new SdEditorInput(modelDir, key), ID);
	}

	@Override
	public void init(
			IEditorSite site, IEditorInput input
	) throws PartInitException {
		super.init(site, input);
		var inp = (SdEditorInput) input;
		modelDir = inp.dir();
		xmile = AppContext.remove(inp.key(), Xmile.class);
		setTitleImage(Icon.SD.get());
		setPartName(inp.getName());
	}

	Xmile xmile() {
		return xmile;
	}

	String modelName() {
		return modelDir.getName();
	}

	File modelDir() {
		return modelDir;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SdInfoPage(this));
			addPage(new SdModelParametersPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to create SD model editor pages", e);
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
