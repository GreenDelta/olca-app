package org.openlca.app.editors.sd.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.AppContext;
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
import org.openlca.sd.xmile.Xmile;

public class SdModelEditor extends FormEditor {

	public static final String ID = "editors.SdModelEditor";

	private final IDatabase db = Database.get();
	private File modelDir;
	private SdModel model;
	private Xmile xmile;
	private boolean dirty;
	private SdGraphEditor graph;

	public static void open(File modelDir) {
		if (modelDir == null || !modelDir.exists() || !modelDir.isDirectory())
			return;
		var xmile = SystemDynamics.openModel(modelDir);
		if (xmile.isError()) {
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

		// load the model from the XMILE file
		var res = SdModel.readFrom(xmile);
		if (res.isError()) {
			MsgBox.error("Failed to read the model", res.error());
			model = new SdModel();
		} else {
			model = res.value();
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

	public Xmile xmile() {
		return xmile;
	}

	String modelName() {
		return modelDir.getName();
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

		var xmileFile = SystemDynamics.getXmileFile(modelDir);
		if (xmileFile == null) {
			MsgBox.error("Failed to save model", "Could not find XMILE file in " + modelDir);
			return;
		}
		var err = model.writeTo(xmileFile);
		if (err.isError()) {
			MsgBox.error("Failed to save model", err.error());
			return;
		}
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
