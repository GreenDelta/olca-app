package org.openlca.app.navigation.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openlca.app.util.SystemDynamics;
import org.openlca.core.database.IDatabase;

public class SdRootElement extends NavigationElement<File> {

	private final IDatabase db;

	public SdRootElement(INavigationElement<?> parent, IDatabase db) {
		super(parent, SystemDynamics.sdRootOf(db).orElse(null));
		this.db = db;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var sds = new ArrayList<INavigationElement<?>>();
		for (var modelDir : SystemDynamics.getModelDirsOf(db)) {
			sds.add(new SdModelElement(this, modelDir));
		}
		return sds;
	}
}
