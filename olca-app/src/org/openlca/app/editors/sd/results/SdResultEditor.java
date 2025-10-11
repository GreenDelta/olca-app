package org.openlca.app.editors.sd.results;

import java.util.ArrayList;
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
import org.openlca.app.util.ErrorReporter;
import org.openlca.commons.Strings;
import org.openlca.sd.eqn.Var;

public class SdResultEditor extends FormEditor {

	public static final String ID = "editors.SdResultEditor";

	private String modelName;
	private CoupledResult result;
	private List<Var> vars;

	public static void open(String modelName, CoupledResult result) {
		if (modelName == null || result == null)
			return;

		var key = AppContext.put(result);
		var input = new SdResultInput(modelName, key);
		Editors.open(input, ID);
	}

	@Override
	public void init(
			IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		var inp = (SdResultInput) input;
		setPartName(inp.getName());
		setTitleImage(Icon.SD.get());

		this.modelName = inp.modelName();
		this.result = AppContext.remove(inp.key());
		this.vars = new ArrayList<>(result.vars());
		this.vars.sort((vi, vj) -> Strings.compareIgnoreCase(
				vi.name().label(), vj.name().label()));
	}

	String modelName() {
		return modelName;
	}

	List<Var> vars() {
		return vars;
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
