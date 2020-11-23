package org.openlca.app.validation;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.openlca.core.database.references.Reference;
import org.openlca.core.model.ModelType;

public class ModelStatus implements Serializable {

	private static final long serialVersionUID = -8610567117130972292L;

	public final ModelType modelType;
	public final long id;
	public final List<Reference> missing;
	public final boolean validReferenceSet;

	ModelStatus(ModelType modelType, long id, List<Reference> missing, boolean validReferenceSet) {
		this.modelType = modelType;
		this.id = id;
		this.missing = Collections.unmodifiableList(missing);
		this.validReferenceSet = validReferenceSet;
	}

	public static enum Status {

		WARNING, ERROR;

	}

	static class Ref {
		ModelType modelType;
		long id;
	}

}
