package org.openlca.app.editors.lcia_methods.shapefiles;

import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.lcia_methods.ImpactMethodEditor;
import org.openlca.app.editors.parameters.ParameterPage;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Error;
import org.openlca.app.util.Info;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.ParameterScope;
import org.openlca.core.model.Uncertainty;
import org.openlca.util.Strings;

class SFAddParamAction extends Action {

	private final SFSection section;

	public SFAddParamAction(SFSection section) {
		this.section = section;
		setToolTipText(M.AddToMethodParameters);
		setText(M.AddToMethodParameters);
		setImageDescriptor(Icon.ADD.descriptor());
	}

	@Override
	public void run() {
		ShapeFileParameter param = getSelected();
		if (param == null)
			return;
		if (exists(param)) {
			Info.showBox(M.ParameterAlreadyAdded,
					M.SelectedParameterWasAlreadyAdded);
			return;
		}
		if (otherExists(param)) {
			Error.showBox(M.ParameterWithSameNameExists,
					M.ParameterWithSameNameExistsInMethod);
			return;
		}
		if (!Parameter.isValidName(param.name)) {
			Error.showBox(M.InvalidParameterName, param.name + " "
					+ M.IsNotValidParameterName);
			return;
		}
		addParam(param);
	}

	private ShapeFileParameter getSelected() {
		if (section == null || section.parameterTable == null)
			return null;
		ShapeFileParameter param = Viewers
				.getFirstSelected(section.parameterTable.viewer);
		if (param == null) {
			Error.showBox(M.NoParameterSelected, M.NoShapefileParameterSelected);
			return null;
		}
		return param;
	}

	private boolean exists(ShapeFileParameter param) {
		for (Parameter realParam : section.method().parameters) {
			if (Strings.nullOrEqual(param.name, realParam.getName())
					&& Strings.nullOrEqual("SHAPE_FILE",
							realParam.getSourceType())
					&& Strings.nullOrEqual(section.shapeFile,
							realParam.getExternalSource()))
				return true;
		}
		return false;
	}

	private boolean otherExists(ShapeFileParameter param) {
		for (Parameter p : section.method().parameters) {
			if (Strings.nullOrEqual(param.name, p.getName())
					&& !Strings.nullOrEqual(section.shapeFile,
							p.getExternalSource()))
				return true;
		}
		return false;
	}

	private void addParam(ShapeFileParameter shapeParam) {
		Parameter p = new Parameter();
		p.setRefId(UUID.randomUUID().toString());
		p.setExternalSource(section.shapeFile);
		p.setInputParameter(true);
		p.setName(shapeParam.name);
		p.setDescription("from shapefile: " + section.shapeFile);
		p.setValue((shapeParam.min + shapeParam.max) / 2);
		p.setUncertainty(Uncertainty.uniform(shapeParam.min,
				shapeParam.max));
		p.setScope(ParameterScope.IMPACT_METHOD);
		p.setSourceType("SHAPE_FILE");
		section.method().parameters.add(p);
		ImpactMethodEditor editor = section.page.editor;
		editor.setDirty(true);
		editor.setActivePage(ParameterPage.ID);
		editor.getParameterSupport().evaluate();
	}
}