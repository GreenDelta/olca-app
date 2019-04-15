package org.openlca.app.tools.mapping;

import java.io.File;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.openlca.app.components.FileChooser;
import org.openlca.app.util.Actions;

public class MappingMenu extends EditorActionBarContributor {

	@Override
	public void contributeToMenu(IMenuManager root) {
		MenuManager menu = new MenuManager("Flow mapping");
		root.add(menu);
		menu.add(Actions.onOpen(() -> {
			File file = FileChooser.forImport("*.zip");
		}));
	}

}
