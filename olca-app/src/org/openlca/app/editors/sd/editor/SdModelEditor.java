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
import org.openlca.app.editors.sd.SdVars;
import org.openlca.app.editors.sd.interop.JsonSetupReader;
import org.openlca.app.editors.sd.interop.JsonSetupWriter;
import org.openlca.app.editors.sd.interop.SimulationSetup;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.commons.Strings;
import org.openlca.core.database.IDatabase;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Vars;
import org.openlca.sd.xmile.Xmile;

public class SdModelEditor extends FormEditor {

	public static final String ID = "editors.SdModelEditor";

	private final IDatabase db = Database.get();
	private File modelDir;
	private Xmile xmile;
	private SimulationSetup setup;
	private List<Var> vars;
	private boolean dirty;

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

		// load the setup
		var setupFile = new File(modelDir, "setup.json");
		setup = setupFile.exists()
				? JsonSetupReader.read(setupFile, db)
				.orElse(SimulationSetup::new)
				: new SimulationSetup();

		// load the model variables
		var varRes = Vars.readFrom(xmile);
		if (varRes.isError()) {
			MsgBox.error("Failed to read variables from model", varRes.error());
			vars = new ArrayList<>();
		} else {
			vars = new ArrayList<>(varRes.value());
			vars.sort((vi, vj) -> {
				int c = Strings.compareIgnoreCase(SdVars.typeOf(vj), SdVars.typeOf(vi));
				if (c != 0)
					return c;
				var li = vi.name() != null ? vi.name().label() : "";
				var lj = vj.name() != null ? vj.name().label() : "";
				return Strings.compareIgnoreCase(li, lj);
			});
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

	Xmile xmile() {
		return xmile;
	}

	String modelName() {
		return modelDir.getName();
	}

	File modelDir() {
		return modelDir;
	}

	SimulationSetup setup() {
		if (setup == null) {
			setup = new SimulationSetup();
		}
		return setup;
	}

	public List<Var> vars() {
		return vars;
	}

	IDatabase db() {
		return db;
	}

	@Override
	protected void addPages() {
		try {
			addPage(new SetupPage(this));
			addPage(new BindingsPage(this));
			addPage(new VarsPage(this));
		} catch (Exception e) {
			ErrorReporter.on("Failed to create SD model editor pages", e);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		var setupFile = new File(modelDir, "setup.json");
		var err = JsonSetupWriter.write(setup, setupFile);
		if (err.isError()) {
			MsgBox.error("Failed to save setup", err.error());
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
