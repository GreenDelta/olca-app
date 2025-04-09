package org.openlca.app.tools.mapping;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.AppContext;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.tools.mapping.model.FlowProvider;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.database.MappingFileDao;
import org.openlca.core.io.maps.FlowMap;
import org.openlca.core.model.MappingFile;
import org.openlca.core.model.ModelType;
import org.openlca.io.Format;
import org.openlca.util.Strings;

public class MappingTool extends SimpleFormEditor {

	FlowMap mapping;
	FlowProvider sourceSystem;
	FlowProvider targetSystem;
	AtomicBoolean checked = new AtomicBoolean(true);

	private MappingPage page;
	private boolean dirty;

	/**
	 * If the mapping is stored in the database, this field contains
	 * the corresponding mapping file.
	 */
	public MappingFile mappingFile;

	public static void createNew() {
		FlowMap mapping = new FlowMap();
		mapping.name = "New flow mapping";
		mapping.refId = UUID.randomUUID().toString();
		open(mapping);
	}

	public static void openFile() {
		var file = FileChooser.open("*.*");
		if (file == null)
			return;
		try {
			var format = Format.detect(file).orElse(null);
			if (format == Format.JSON_LD_ZIP) {
				open(JsonImportDialog.open(file));
			} else if (format == Format.MAPPING_CSV) {
				open(FlowMap.fromCsv(file));
			} else {
				MsgBox.info(M.UnsupportedFileFormatInfo);
			}
		} catch (Exception e) {
			MsgBox.error(M.CouldNotOpenFile, e);
		}
	}

	/**
	 * Open an existing mapping from the database.
	 */
	public static void open(MappingFile mapping) {
		if(mapping == null)
			return;
		var uid = "db:mapping/" + mapping.id;
		AppContext.put(uid, mapping);
		Editors.open(
				new SimpleEditorInput(uid, mapping.name),
				"MappingTool");
	}

	public static void open(FlowMap mapping) {
		if (mapping == null)
			return;
		String uid = mapping.refId;
		if (uid == null) {
			uid = UUID.randomUUID().toString();
			mapping.refId = uid;
		}		
		var cacheID = uid + " /mapping";
		AppContext.put(cacheID, mapping);
		Editors.open(
				new SimpleEditorInput(cacheID, M.FlowMapping),
				"MappingTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		setTitleImage(Images.get(ModelType.FLOW));
		try {
			var inp = (SimpleEditorInput) input;
			var raw = AppContext.remove(inp.id);
			if (raw instanceof FlowMap) {
				mapping = (FlowMap) raw;
			} else if (raw instanceof MappingFile) {
				mappingFile = (MappingFile) raw;
				mapping = FlowMap.of(mappingFile);
			} else {
				mapping = FlowMap.empty();
			}
			checked.set(mapping.entries.isEmpty());
		} catch (Exception e) {
			ErrorReporter.on("Failed to initialize mapping tool", e);
		}
	}

	protected void setDirty() {
		// the dirty state can only change if this is
		// a mapping from the database
		if (mappingFile == null)
			return;
		dirty = true;
		editorDirtyStateChanged();
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		if (mappingFile == null)
			return;
		var db = Database.get();
		if (db == null)
			return;
		try {
			mapping.updateContentOf(mappingFile);
			new MappingFileDao(db).update(mappingFile);
			dirty = false;
			editorDirtyStateChanged();
		} catch (Exception e) {
			ErrorReporter.on("Failed to save mapping file "
					+ mappingFile.name, e);
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public void doSaveAs() {
		var db = Database.get();
		if (db == null)
			return;

		// guess a new mapping name
		var dao = new MappingFileDao(db);
		var existing = dao.getNames()
				.stream()
				.map(String::toLowerCase)
				.collect(Collectors.toSet());
		String proposedName;
		var i = existing.size();
		do {
			i++;
			proposedName = "flow_mapping_" + i + ".csv";
		} while (existing.contains(proposedName));

		// open a friendly dialog
		var dialog = new InputDialog(
				UI.shell(),
				M.SaveMappingInDatabase,
				M.SaveMappingInDatabaseInfo,
				proposedName,
				name -> {
					if (Strings.nullOrEmpty(name))
						return M.NameCannotBeEmpty;
					if (existing.contains(name.toLowerCase().trim()))
						return M.FlowMappingWithThisNameExists;
					return null;
				});
		if (dialog.open() != Window.OK)
			return;

		// store it under this name
		try {
			var name = dialog.getValue().trim();
			var newMapping = new MappingFile();
			newMapping.name = name;
			mapping.updateContentOf(newMapping);
			dao.insert(newMapping);
			Navigator.refresh();

			// set it as the mapping file of this editor
			// if applicable
			if (mappingFile == null) {
				mappingFile = newMapping;
			}
		} catch (Exception e) {
			ErrorReporter.on("Failed to save mapping file in database", e);
		}
	}

	@Override
	protected FormPage getPage() {
		page = new MappingPage(this);
		return page;
	}

	/**
	 * Refreshes the flow mapping in the page. This method can be called when the
	 * mapping was modified outside the page (e.g. in a process that generates new
	 * mappings).
	 */
	void refresh() {
		if (page != null && page.table != null) {
			page.table.setInput(mapping.entries);
		}
	}
}
