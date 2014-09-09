package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.App;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.ImageType;
import org.openlca.io.ecospold2.input.MarketProcessCleanUp;

public class XEI3MarketProcessCleanUp extends Action implements
		INavigationAction {

	public XEI3MarketProcessCleanUp() {
		setImageDescriptor(ImageType.EXTENSION_ICON.getDescriptor());
		setText("Merge EI3 market processes");
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof DatabaseElement))
			return false;
		DatabaseElement e = (DatabaseElement) element;
		return Database.isActive(e.getContent());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		return false;
	}

	@Override
	public void run() {
		App.run("Merge market processes",
				new MarketProcessCleanUp(Database.get()));
	}

}
