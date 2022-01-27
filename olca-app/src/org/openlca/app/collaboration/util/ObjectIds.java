package org.openlca.app.collaboration.util;

import org.eclipse.jgit.lib.ObjectId;

public class ObjectIds {

	public static boolean nullOrZero(ObjectId id) {
		return id == null || id.equals(ObjectId.zeroId());
	}

}
