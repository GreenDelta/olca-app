package org.openlca.app.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;

public class ExportAction extends Action implements INavigationAction {

	public ExportAction() {
		setText(M.Export);
		setImageDescriptor(Icon.EXPORT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		for (INavigationElement<?> e : selection) {
			if (e instanceof ModelTypeElement
					|| e instanceof CategoryElement
					|| e instanceof ModelElement)
				return true;
			if (e instanceof DatabaseElement elem) {
				if (Database.isActive(elem.getContent()))
					return true;
			}
		}
		return false;
	}

	@Override
	public void run() {
		if (Database.get() == null) {
			MsgBox.info("No opened database",
					"You need to open the database for the export first.");
			return;
		}
		try {
			var service = PlatformUI.getWorkbench()
					.getService(IHandlerService.class);
			service.executeCommand(ActionFactory.EXPORT.getCommandId(), null);
		} catch (Exception e) {
			ErrorReporter.on("Failed to open export wizard", e);
		}
	}

}
