package org.openlca.app.editors.sd;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.AppContext;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.sd.eqn.Var;

public class SdResultEditor extends FormEditor {

	public static final String ID = "editors.SdResultEditor";

	private String modelName;
	private List<Var> variables;

	public static void open(String modelName, List<Var> variables) {
		if (variables == null || variables.isEmpty())
			return;
		var key = AppContext.put(variables);
		var input = new SdResultInput(modelName, variables, key);
		org.openlca.app.editors.Editors.open(input, ID);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setTitleImage(Images.get(FileType.MARKUP));
		var inp = (SdResultInput) input;
		modelName = inp.modelName();
		variables = AppContext.remove(inp.key(), List.class);
	}

	String modelName() {
		return modelName;
	}

	List<Var> variables() {
		return variables;
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
