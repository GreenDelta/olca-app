package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.tools.mapping.MappingTool;
import org.openlca.app.util.ErrorReporter;
import org.openlca.core.database.MappingFileDao;

public class OpenMappingAction extends Action implements INavigationAction {

	private String mappingFile;

	public OpenMappingAction() {
		setText(M.Open);
		setImageDescriptor(Icon.FOLDER_OPEN.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection == null || selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (!(first instanceof MappingFileElement))
			return false;
		mappingFile = ((MappingFileElement) first).getContent();
		return mappingFile != null;
	}

	@Override
	public void run() {
		run(mappingFile);
	}

	/**
	 * Open the mapping file with the given name from the currently
	 * active database.
	 */
	public static void run(String mappingFile) {
		var db = Database.get();
		if (db == null || mappingFile == null)
			return;
		var mapping = new MappingFileDao(db)
				.getForName(mappingFile);
		if (mapping == null){
			ErrorReporter.on("Mapping file "
					+ mappingFile + " does not exist");
			return;
		}
		try {
			MappingTool.open(mapping);
		} catch (Exception e) {
			ErrorReporter.on("Failed to open " +
					"mapping file: " + mappingFile,e );
		}
	}
}
