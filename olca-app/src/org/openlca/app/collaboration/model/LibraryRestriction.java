package org.openlca.app.collaboration.model;

import org.openlca.git.model.Reference;

public class LibraryRestriction {

	public final String datasetRefId;
	public final String library;
	public final RestrictionType type;
	public Reference ref;

	public LibraryRestriction(String datasetRefId, String library, RestrictionType type) {
		this.datasetRefId = datasetRefId;
		this.library = library;
		this.type = type;
	}

	public enum RestrictionType {

		WARNING,

		FORBIDDEN;

	}

}
