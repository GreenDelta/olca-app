package org.openlca.app.editors;

import java.util.ArrayList;
import java.util.List;

import org.openlca.app.editors.lcia_methods.shapefiles.ShapeFileParameter;

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
			if (parameter.name.equals(name))
				return parameter;
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof ExternalSource))
			return false;
		ExternalSource other = (ExternalSource) obj;
		String source = getSource() != null ? getSource() : "";
		String type = getType() != null ? getType() : "";
		if (!type.equals(other.getType()))
			return false;
		if (!source.equals(other.getSource()))
			return false;
		return true;
	}
}
