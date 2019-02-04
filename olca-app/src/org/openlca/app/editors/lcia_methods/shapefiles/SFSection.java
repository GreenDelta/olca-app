package org.openlca.app.editors.lcia_methods.shapefiles;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Actions;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactMethod;
import org.openlca.core.model.Parameter;
import org.openlca.core.model.Uncertainty;

class SFSection {

	final String shapeFile;
	final int index;
	final ShapeFilePage page;

	private Section section;
	SFParameterTable parameterTable;

	SFSection(ShapeFilePage page, int index, String shapeFile) {
		this.page = page;
		this.index = index;
		this.shapeFile = shapeFile;
	}

	void render(Composite body, FormToolkit tk) {
		section = UI.section(body, tk, M.Parameters + " - " + shapeFile);
		Composite comp = UI.sectionClient(section, tk, 1);
		parameterTable = new SFParameterTable(page.editor, shapeFile, comp);
		Action delete = Actions.onRemove(() -> {
			if (delete(false)) {
				removeExternalSourceReferences();
				page.editor.doSave(null);
			}
		});
		Action update = new Update();
		SFShowMapAction showAction = new SFShowMapAction(this);
		SFAddParamAction addAction = new SFAddParamAction(this);
		Actions.bind(section, showAction, delete, update);
		Actions.bind(parameterTable.viewer, showAction, addAction);
	}

	ImpactMethod method() {
		return page.editor.getModel();
	}

	private boolean delete(boolean force) {
		boolean del = force
				|| Question.ask(M.DeleteShapeFile, M.ReallyDeleteShapeFile);
		if (!del)
			return false;
		ShapeFileUtils.deleteFile(method(), shapeFile);
		section.dispose();
		page.removeSection(this);
		return true;
	}

	private void removeExternalSourceReferences() {
		for (Parameter parameter : method().parameters) {
			if (!parameter.isInputParameter)
				continue;
			if (shapeFile.equals(parameter.externalSource)) {
				parameter.externalSource = null;
			}
		}
		page.editor.getParameterSupport().evaluate();
		page.editor.setDirty(true);
	}

	/*
	 * stillLinked: names of parameters that were linked to the shape file
	 * before updating and afterwards (only set those)
	 */
	private void updateExternalSourceReferences(Set<String> stillLinked,
			Map<String, ShapeFileParameter> nameToParam) {
		for (Parameter parameter : method().parameters) {
			if (!parameter.isInputParameter)
				continue;
			if (!shapeFile.equals(parameter.externalSource))
				continue;
			if (!stillLinked.contains(parameter.name)) {
				parameter.externalSource = null;
			} else {
				ShapeFileParameter param = nameToParam.get(parameter.name);
				if (param == null)
					continue;
				parameter.value = (param.min + param.max) / 2;
				parameter.uncertainty = Uncertainty.uniform(
						param.min, param.max);
			}
		}
		page.editor.getParameterSupport().evaluate();
	}

	private Set<String> getReferencedParameters() {
		Set<String> names = new HashSet<>();
		for (Parameter parameter : method().parameters)
			if (parameter.isInputParameter)
				if (shapeFile.equals(parameter.externalSource))
					names.add(parameter.name);
		return names;
	}

	private class Update extends Action {

		Update() {
			setText(M.Update);
			setToolTipText(M.Update);
			setImageDescriptor(Icon.REFRESH.descriptor());
		}

		@Override
		public void run() {
			File file = FileChooser.forImport("*.shp");
			if (file == null)
				return;
			Set<String> previouslyLinked = getReferencedParameters();
			delete(true);
			List<ShapeFileParameter> parameters = page.checkRunImport(file);
			Set<String> stillLinked = getReferencedParameters();
			Map<String, ShapeFileParameter> nameToParam = new HashMap<>();
			for (ShapeFileParameter parameter : parameters)
				if (previouslyLinked.contains(parameter.name)) {
					stillLinked.add(parameter.name);
					nameToParam.put(parameter.name, parameter);
				}
			updateExternalSourceReferences(stillLinked, nameToParam);
			page.editor.doSave(null);
		}
	}
}