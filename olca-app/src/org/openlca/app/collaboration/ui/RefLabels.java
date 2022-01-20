package org.openlca.app.collaboration.ui;

import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.Daos;
import org.openlca.git.model.Reference;
import org.openlca.git.util.ObjectIds;

public class RefLabels {

	public static String getFullName(Reference ref) {
		if (ref.category.isEmpty())
			return getName(ref);
		return ref.category + "/" + getName(ref);
	}

	@SuppressWarnings("resource")
	public static String getName(Reference ref) {
		// TODO optimize
		if (ObjectIds.isNullOrZero(ref.objectId)) {
			return Daos.categorized(Database.get(), ref.type).getDescriptorForRefId(ref.refId).name;
		}
		return Repository.get().datasets.getName(ref.objectId);
	}

}
