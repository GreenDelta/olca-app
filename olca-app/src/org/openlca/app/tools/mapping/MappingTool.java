package org.openlca.app.tools.mapping;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.AppCache;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.model.ProviderType;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.core.model.MappingFile;
import org.openlca.io.maps.FlowMap;

public class MappingTool extends SimpleFormEditor {

	FlowMap mapping;
	IProvider sourceSystem;
	IProvider targetSystem;
	AtomicBoolean checked = new AtomicBoolean(true);

	private MappingPage page;

	/**
	 * If the mapping is stored in the database, this field contains
	 * the corresponding mapping file.
	 */
	private MappingFile mappingFile;

	public static void createNew() {
		FlowMap mapping = new FlowMap();
		mapping.name = "New flow mapping";
		mapping.refId = UUID.randomUUID().toString();
		open(mapping);
	}

	public static void openFile() {
		File file = FileChooser.open("*.*");
		if (file == null)
			return;
		try {
			ProviderType type = ProviderType.of(file);
			if (type == ProviderType.JSON_LD_PACKAGE) {
				open(JsonImportDialog.open(file));
			} else if (type == ProviderType.CSV_FILE) {
				open(FlowMap.fromCsv(file));
			} else {
				MsgBox.info("Unsupported format file format. Supported are "
						+ "flow mappings from CSV files and JSON-LD packages.");
			}
		} catch (Exception e) {
			MsgBox.error("Could not open file", e.getMessage());
		}
	}

	/**
	 * Open an existing mapping from the database.
	 */
	public static void open(MappingFile mapping) {
		if(mapping == null)
			return;
		var uid = "db:mapping/" + mapping.id;
		Cache.getAppCache().put(uid, mapping);
		Editors.open(
				new SimpleEditorInput(
						"FlowMappings", uid, mapping.name),
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
		AppCache cache = Cache.getAppCache();
		var cacheID = uid + " /mapping";
		cache.put(cacheID, mapping);
		Editors.open(
				new SimpleEditorInput(
						"FlowMappings", cacheID, "Flow mapping"),
				"MappingTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			var inp = (SimpleEditorInput) input;
			var raw = Cache.getAppCache().remove(inp.id);
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

	@Override
	protected FormPage getPage() {
		page = new MappingPage(this);
		return page;
	}

	/**
	 * Refreshes the flow mapping in the page. This method can be called when
	 * the mapping was modified outside of the page (e.g. in a process that
	 * generates new mappings).
	 */
	void refresh() {
		if (page != null && page.table != null) {
			page.table.setInput(mapping.entries);
		}
	}
}
