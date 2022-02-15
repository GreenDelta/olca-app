package org.openlca.app.collaboration.util;

import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.Daos;
import org.openlca.git.model.Reference;

public class RefLabels {

	public static String getFullName(Reference ref) {
		if (ref.category.isEmpty())
			return getName(ref);
		return ref.category + "/" + getName(ref);
	}

	@SuppressWarnings("resource")
	public static String getName(Reference ref) {
		// TODO optimize
		if (ObjectIds.nullOrZero(ref.objectId)) {
			return Daos.categorized(Database.get(), ref.type).getDescriptorForRefId(ref.refId).name;
		}
		return Repository.get().datasets.getName(ref.objectId);
	}

}
