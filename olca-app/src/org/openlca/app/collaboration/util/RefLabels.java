package org.openlca.app.collaboration.util;

import org.eclipse.jgit.lib.ObjectId;
import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.core.database.Daos;
import org.openlca.git.model.ModelRef;

public class RefLabels {

	public static String getFullName(ModelRef ref, ObjectId objectId) {
		var name = getName(ref, objectId);
		if (ref.category.isEmpty())
			return name;
		return ref.category + "/" + name;
	}

	@SuppressWarnings("resource")
	public static String getName(ModelRef ref, ObjectId objectId) {
		// TODO optimize
		if (ObjectIds.nullOrZero(objectId)) {
			return Daos.root(Database.get(), ref.type).getDescriptorForRefId(ref.refId).name;
		}
		return Repository.get().datasets.getName(objectId);
	}

}
