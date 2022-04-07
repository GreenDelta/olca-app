package org.openlca.app.collaboration.model;

import org.openlca.core.model.ModelType;

public class LibraryRestriction {

	public final String datasetRefId;
	public final String library;
	public final RestrictionType type;
	public ModelType modelType;
	public String path;

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
