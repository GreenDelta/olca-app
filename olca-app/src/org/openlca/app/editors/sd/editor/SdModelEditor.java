package org.openlca.app.editors.sd.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormEditor;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.sd.editor.graph.SdGraphEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.sd.model.SdModel;

import java.io.File;

public class SdModelEditor extends FormEditor {

	public static final String ID = "editors.SdModelEditor";

	private final IDatabase db = Database.get();
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
		var modelDir = inp.dir();

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

		setTitleImage(Icon.SD.get());
		if (Strings.isNotBlank(model.name())) {
			setPartName(model.name());
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

	IDatabase db() {
		return db;
	}

	@Override
	protected void addPages() {
		try {
			graph = new SdGraphEditor(this);
			int index = addPage(
				graph, new SimpleEditorInput(model.id(), model.name()));
			setPageText(index, "Model");

			addPage(new SetupPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to create SD model editor pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (graph != null) {
			graph.doSave(monitor);
		}
		var res = SystemDynamics.saveModel(model, db);
		if (res.isError()) {
			MsgBox.error("Failed to save model", res.error());
			return;
		}
		if (Strings.isNotBlank(model.name())) {
			setPartName(model.name());
		}
		dirty = false;
		editorDirtyStateChanged();
		Navigator.refresh();
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
}
