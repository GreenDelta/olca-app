package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.editors.lcia_methods.ShapeFileParameter;

public class ExternalSource {

	private String source;
	private String type;
	private List<ShapeFileParameter> providedParameters = new ArrayList<>();

	public ExternalSource(String source, String type,
			List<ShapeFileParameter> providedParameters) {
		this.source = source;
		this.type = type;
		this.providedParameters = providedParameters;
	}

	public String getSource() {
		return source;
	}

	public String getType() {
		return type;
	}

	public boolean isProvidingParameter(String name) {
		return getParameter(name) != null;
	}

	public ShapeFileParameter getParameter(String name) {
		for (ShapeFileParameter parameter : this.providedParameters)
			if (parameter.getName().equals(name))
				return parameter;
		return null;
	}

}
