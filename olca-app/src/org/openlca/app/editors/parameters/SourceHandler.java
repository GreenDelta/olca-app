package org.openlca.app.editors.parameters;

import org.openlca.core.model.Parameter;

public interface SourceHandler {

	String[] getSources(Parameter parameter);
	
	void sourceChanged(Parameter parameter, String source);
	
}
