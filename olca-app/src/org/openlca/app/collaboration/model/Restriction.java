package org.openlca.app.collaboration.model;

import org.openlca.core.model.ModelType;

public class Restriction {

	public final String datasetRefId;
	public final String name;
	public final RestrictionType type;
	public ModelType modelType;
	public String path;

	public Restriction(String datasetRefId, String name, RestrictionType type) {
		this.datasetRefId = datasetRefId;
		this.name = name;
		this.type = type;
	}

	public enum RestrictionType {

		WARNING,

		FORBIDDEN;

	}

}
