package org.openlca.app.navigation.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Database;

public class SdRootElement extends NavigationElement<Void> {

	public SdRootElement(INavigationElement<?> parent) {
		super(parent, null);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var db = Database.get();
		if (db == null)
			return List.of();
		var dir = db.getFileStorageLocation();
		if (dir == null || !dir.exists())
			return List.of();
		var sdRoot = new File(dir, "sd-models");
		if (!sdRoot.exists())
			return List.of();
		var modelDirs = sdRoot.listFiles();
		if (modelDirs == null)
			return List.of();
		var sds = new ArrayList<INavigationElement<?>>(modelDirs.length);
		for (var modelDir : modelDirs) {
			sds.add(new SdModelElement(this, modelDir));
		}
		return sds;
	}
}
