package org.openlca.app.editors.sd.results;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.sd.interop.CoupledResult;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.sd.eqn.Var;

public class SdResultEditor extends FormEditor {

	public static final String ID = "editors.SdResultEditor";

	private String modelName;
	private CoupledResult result;

	public static void open(String modelName, CoupledResult result) {
		if (modelName == null || result == null)
			return;

		var key = AppContext.put(result);
		var input = new SdResultInput(modelName, key);
		Editors.open(input, ID);
	}

	@Override
	public void init(
			IEditorSite site, IEditorInput input
	) throws PartInitException {
		super.init(site, input);
		setTitleImage(Images.get(FileType.MARKUP));
		var inp = (SdResultInput) input;
		modelName = inp.modelName();
		result = AppContext.remove(inp.key());
		setTitleImage(Icon.SD.get());
		setPartName(inp.getName());
	}

	String modelName() {
		return modelName;
	}

	List<Var> vars() {
		return result.getVariables();
	}

	CoupledResult result() {
		return result;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SdResultPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to create SD result editor page", e);
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
