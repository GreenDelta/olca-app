package org.openlca.app.editors.sd;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.App;
import org.openlca.app.AppContext;
import org.openlca.app.editors.Editors;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.FileType;
import org.openlca.app.util.MsgBox;
import org.openlca.sd.eqn.Simulator;
import org.openlca.sd.eqn.Var;
import org.openlca.util.Strings;

public class SdResultEditor extends FormEditor {

	public static final String ID = "editors.SdResultEditor";

	private String modelName;
	private List<Var> vars;

	public static void open(String modelName, Simulator simulator) {
		if (modelName == null || simulator == null)
			return;
		var errRef = new AtomicReference<String>();
		App.exec("Run simulation ....", () -> simulator.forEach(res -> {
			if (res.hasError()) {
				errRef.set(res.error());
			}
		}));

		var err = errRef.get();
		if (Strings.notEmpty(err)) {
			MsgBox.error("Simulation failed", err);
			return;
		}

		var key = AppContext.put(simulator.vars());
		var input = new SdResultInput(modelName, key);
		Editors.open(input, ID);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		setTitleImage(Images.get(FileType.MARKUP));
		var inp = (SdResultInput) input;
		modelName = inp.modelName();
		vars = AppContext.remove(inp.key(), List.class);
		if (vars == null) {
			throw new PartInitException("No simulation result found in context");
		}
		setTitleImage(Icon.SD.get());
		setPartName(inp.getName());
	}

	String modelName() {
		return modelName;
	}

	List<Var> vars() {
		return vars;
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
