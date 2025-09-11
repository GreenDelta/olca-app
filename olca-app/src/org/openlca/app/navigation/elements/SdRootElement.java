package org.openlca.app.navigation.elements;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.db.Database;
import org.openlca.app.util.SystemDynamics;

public class SdRootElement extends NavigationElement<Void> {

	public SdRootElement(INavigationElement<?> parent) {
		super(parent, null);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
			var sds = new ArrayList<INavigationElement<?>>();
		for (var modelDir : SystemDynamics.getModelDirsOf(Database.get())) {
			sds.add(new SdModelElement(this, modelDir));
		}
		return sds;
	}
}
