package org.openlca.app.editors.lcia_methods.shapefiles;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.viewers.Viewers;

class SFShowMapAction extends Action {

	private final SFSection section;

	public SFShowMapAction(SFSection section) {
		this.section = section;
		setToolTipText(M.ShowInMap);
		setText(M.ShowInMap);
		setImageDescriptor(Icon.MAP.descriptor());
	}

	@Override
	public void run() {
		if (section == null || section.parameterTable == null)
			return;
		ShapeFileParameter param = Viewers
				.getFirstSelected(section.parameterTable.viewer);
		if (param == null)
			ShapeFileUtils.openFileInMap(section.impact(), section.shapeFile);
		else
			ShapeFileUtils.openFileInMap(section.impact(), section.shapeFile, param);
	}
}