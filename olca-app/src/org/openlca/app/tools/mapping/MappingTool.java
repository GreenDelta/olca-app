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
import org.openlca.io.maps.FlowMap;

public class MappingTool extends SimpleFormEditor {

	private MappingPage page;

	FlowMap mapping;
	IProvider sourceSystem;
	IProvider targetSystem;
	AtomicBoolean checked = new AtomicBoolean(true);

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

	public static void open(FlowMap mapping) {
		if (mapping == null)
			return;
		String uid = mapping.refId;
		if (uid == null) {
			uid = UUID.randomUUID().toString();
			mapping.refId = uid;
		}
		AppCache cache = Cache.getAppCache();
		cache.put(uid + " /mapping", mapping);
		Editors.open(new SimpleEditorInput(
				"FlowMappings", uid, "Flow mapping"),
				"MappingTool");
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		try {
			SimpleEditorInput sinp = (SimpleEditorInput) input;
			String uid = sinp.id;
			AppCache cache = Cache.getAppCache();
			mapping = cache.remove(uid + " /mapping", FlowMap.class);
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
