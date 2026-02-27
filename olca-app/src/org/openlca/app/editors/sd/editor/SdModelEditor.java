package org.openlca.app.editors.sd.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.sd.SdVars;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.sd.model.SdModel;
import org.openlca.sd.model.Var;

public class SdModelEditor extends FormEditor {

	public static final String ID = "editors.SdModelEditor";

	private final IDatabase db = Database.get();
	private File modelDir;
	private SdModel model;
	private boolean dirty;
	private SdGraphEditor graph;

	public static void open(File dir) {
		if (dir == null || !dir.exists() || !dir.isDirectory())
			return;
		var file = SystemDynamics.getXmileFile(dir);
		if (file == null || !file.exists()) {
			MsgBox.error("Failed to read model",
					"No XMILE file found in: " + dir);
			return;
		}
		Editors.open(SdEditorInput.of(dir), ID);
	}

	@Override
	public void init(
			IEditorSite site, IEditorInput input
	) throws PartInitException {
		super.init(site, input);
		var inp = (SdEditorInput) input;
		modelDir = inp.dir();
		setTitleImage(Icon.SD.get());
		setPartName(inp.getName());

		// load the model from the XMILE file
		var file = SystemDynamics.getXmileFile(modelDir);
		if (file == null || !file.exists()) {
			MsgBox.error("Failed to read the model",
					"No XMILE file in: " + modelDir);
			model = new SdModel();
		} else {
			var res = SdModel.readFrom(file);
			if (res.isError()) {
				MsgBox.error("Failed to read the model", res.error());
				model = new SdModel();
			} else {
				model = res.value();
			}
		}
	}

	public void setDirty() {
		dirty = true;
		editorDirtyStateChanged();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	public SdModel model() {
		return model;
	}

	File modelDir() {
		return modelDir;
	}

	public List<Var> vars() {
		var vars = new ArrayList<>(model.vars());
		vars.sort((vi, vj) -> {
			int c = Strings.compareIgnoreCase(SdVars.typeOf(vj), SdVars.typeOf(vi));
			if (c != 0)
				return c;
			var li = vi.name() != null ? vi.name().label() : "";
			var lj = vj.name() != null ? vj.name().label() : "";
			return Strings.compareIgnoreCase(li, lj);
		});
		return vars;
	}

	IDatabase db() {
		return db;
	}

	@Override
	protected void addPages() {
		try {
			graph = new SdGraphEditor(this);
			var gInput = new GraphicalEditorInput(null);
			int index = addPage(graph, gInput);
			setPageText(index, "Model");

			addPage(new SetupPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to create SD model editor pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (graph != null) {
			graph.syncTo(model);
			graph.doSave(monitor);
		}

		// determine the target file; rename if the model name changed
		var currentFile = SystemDynamics.getXmileFile(modelDir);
		var targetName = SystemDynamics.sanitizeName(model.name()) + ".xml";
		var targetFile = new File(modelDir, targetName);
		if (currentFile != null && currentFile.exists()
				&& !currentFile.getName().equals(targetName)) {
			// name changed — delete old file (we write fresh below)
			currentFile.delete();
		}

		var err = model.writeTo(targetFile);
		if (err.isError()) {
			MsgBox.error("Failed to save model", err.error());
			return;
		}
		setPartName(Objects.requireNonNullElse(
			model.name(), "System dynamics model"));
		dirty = false;
		editorDirtyStateChanged();
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
}
