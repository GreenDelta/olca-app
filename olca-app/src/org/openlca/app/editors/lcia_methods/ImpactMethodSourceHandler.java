package org.openlca.app.editors.lcia_methods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openlca.app.editors.parameters.SourceHandler;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Uncertainty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImpactMethodSourceHandler implements SourceHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final ImpactMethodEditor editor;

	public ImpactMethodSourceHandler(ImpactMethodEditor editor) {
		this.editor = editor;
	}

	@Override
	public String[] getSources(Parameter parameter) {
		List<String> names = ShapeFileUtils.getShapeFiles(editor.getModel());
		List<String> sources = new ArrayList<>();
		try {
			for (String name : names) {
				List<ShapeFileParameter> params = ShapeFileUtils.getParameters(
						editor.getModel(), name);
				for (ShapeFileParameter param : params)
					if (param.getName().equals(parameter.getName()))
						sources.add(name);
			}
		} catch (IOException e) {
			log.error("Error loading shape file parameter names", e);
		}
		return sources.toArray(new String[sources.size()]);
	}

	@Override
	public void sourceChanged(Parameter parameter, String source) {
		ShapeFileParameter sfParam = null;
		try {
			List<ShapeFileParameter> sfParams = ShapeFileUtils.getParameters(
					editor.getModel(), source);
			for (ShapeFileParameter sfp : sfParams) {
				if (parameter.getName().equals(sfp.getName()))
					sfParam = sfp;
			}
		} catch (IOException e) {
			log.error("Error loading shape file parameter names", e);
		}
		if (sfParam == null)
			return;
		parameter.setRefId(UUID.randomUUID().toString());
		parameter.setExternalSource(source);
		parameter.setInputParameter(true);
		parameter.setDescription("from shapefile: " + source);
		parameter.setValue((sfParam.getMin() + sfParam.getMax()) / 2);
		parameter.setUncertainty(Uncertainty.uniform(sfParam.getMin(),
				sfParam.getMax()));
		parameter.setScope(ParameterScope.IMPACT_METHOD);
		parameter.setSourceType("SHAPE_FILE");
		editor.getParameterSupport().evaluate();
	}

}
