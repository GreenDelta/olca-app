package org.openlca.app.tools.mapping;

import java.io.File;
import java.util.UUID;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.FormPage;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.AppCache;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.editors.Editors;
import org.openlca.app.editors.SimpleEditorInput;
import org.openlca.app.editors.SimpleFormEditor;
import org.openlca.app.tools.mapping.model.DBProvider;
import org.openlca.app.tools.mapping.model.FlowMap;
import org.openlca.app.tools.mapping.model.ILCDProvider;
import org.openlca.app.tools.mapping.model.IProvider;
import org.openlca.app.tools.mapping.model.Matcher;
import org.openlca.app.tools.mapping.model.ProviderType;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;

public class MappingTool extends SimpleFormEditor {

	private MappingPage page;

	FlowMap mapping;
	IProvider provider;

	public static void createNew() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		FlowMap mapping = new FlowMap();
		mapping.name = "New flow mapping";
		mapping.refId = UUID.randomUUID().toString();
		open(mapping, new DBProvider(Database.get()));
	}

	public static void openFile() {
		if (Database.get() == null) {
			Info.showBox(M.NoDatabaseOpened, M.NeedOpenDatabase);
			return;
		}
		// TODO: support CSV files and ILCD packages
		File file = FileChooser.forImport("*.zip");
		if (file == null)
			return;
		try {
			ProviderType type = ProviderType.of(file);
			if (type == ProviderType.JSON_LD_PACKAGE) {
				JsonImportDialog.open(file);
			} else if (type == ProviderType.ILCD_PACKAGE) {
				Matcher.generate(ILCDProvider.of(file), Database.get());
			} else {
				Info.showBox("#Unsupported format.");
			}
		} catch (Exception e) {
			Error.showBox("Could not open file", e.getMessage());
		}
	}

	public static void generate(IProvider provider) {

	}

	public static void open(FlowMap mapping, IProvider provider) {
		if (mapping == null || provider == null)
			return;
		String uid = mapping.refId;
		if (uid == null) {
			uid = UUID.randomUUID().toString();
			mapping.refId = uid;
		}
		AppCache cache = Cache.getAppCache();
		cache.put(uid + " /mapping", mapping);
		cache.put(uid + " /provider", provider);
		Editors.open(new SimpleEditorInput(
				"FlowMappings", uid, "#Flow mapping"),
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
			provider = cache.remove(uid + " /provider", IProvider.class);
		} catch (Exception e) {
			throw new PartInitException(
					"Failed to load editor input", e);
		}
	}

	@Override
	protected FormPage getPage() {
		page = new MappingPage(this);
		return page;
	}
}
