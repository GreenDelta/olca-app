package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.MappingFileDao;
import org.openlca.io.maps.FlowMap;

public class ExportFlowMapAction extends Action implements INavigationAction {

	private MappingFileElement elem;

	public ExportFlowMapAction() {
		setText(M.Export);
		setImageDescriptor(Icon.EXPORT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof MappingFileElement))
			return false;
		this.elem = (MappingFileElement) first;
		return true;
	}

	@Override
	public void run() {
		var db = Database.get();
		if (elem == null || db == null)
			return;
		var name = elem.getContent();
		if (name == null)
			return;

		// select a target file
		var proposal = name.endsWith(".csv")
			? name
			: name + ".csv";
		var target = FileChooser.forSavingFile(M.Export, proposal);
		if (target == null)
			return;

		// write the file
		try {
			var mapping = new MappingFileDao(db).getForName(name);
			if (mapping == null)
				return;
			var flowMap = FlowMap.of(mapping);
			FlowMap.toCsv(flowMap, target);
		} catch (Exception e) {
			ErrorReporter.on("Failed to export mapping: " + name, e);
		}
	}
}
