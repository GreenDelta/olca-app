package org.openlca.app.collaboration.model;

import java.util.Date;

import org.openlca.core.model.ModelType;

public record Comment(long id, String user, String text, String refId, ModelType type, String path, Date date,
		boolean released, boolean approved, long replyTo) {

}
