package org.openlca.app.devtools;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.devtools.sql.SqlEditor;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.util.ErrorReporter;

public abstract class ScriptingEditor extends SimpleFormEditor {

	protected File file;
	protected String script = "";
	private boolean _dirty;

	public abstract void eval();

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		if (!(input instanceof SimpleEditorInput))
			return;
		var id = ((SimpleEditorInput) input).id;
		if (id.endsWith("_new"))
			return;
		var file = new File(id);
		if (!file.exists())
			return;
		this.file = file;
		try {
			script = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			setPartName(file.getName());
		} catch (Exception e) {
			ErrorReporter.on("Failed to read script " + file, e);
		}
	}

	protected void setDirty() {
		// can only set the editor dirty if there is a file
		if (file == null)
			return;
		_dirty = true;
		editorDirtyStateChanged();
	}

	@Override
	public boolean isDirty() {
		return _dirty;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (file == null)
			return;
		try {
			var dir = file.getParentFile().toPath();
			Files.createDirectories(dir);
			Files.writeString(
					file.toPath(),
					script,
					StandardCharsets.UTF_8);
			_dirty = false;
			editorDirtyStateChanged();
		} catch (Exception e) {
			ErrorReporter.on("Failed to save script "
					+ file + ": " + e.getMessage(), e);
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSaveAs() {
		var ext = this instanceof SqlEditor
				? ".sql"
				: ".py";
		String name = "script" + ext;
		if (file != null) {
			name = file.getName();
			if (name.endsWith(ext)) {
				name = name.substring(0, name.length() - ext.length());
			}
			name += "_copy" + ext;
		}
		var newFile = SaveScriptDialog.forScriptOf(name, script)
				.orElse(null);
		if (file == null && newFile != null) {
			file = newFile;
			setPartName(file.getName());
		}
	}
}
