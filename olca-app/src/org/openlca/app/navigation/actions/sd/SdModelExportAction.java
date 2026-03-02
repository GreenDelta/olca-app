package org.openlca.app.navigation.actions.sd;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.components.FileChooser;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;

public class SdModelExportAction extends Action implements INavigationAction {

	private SdModelElement elem;

	public SdModelExportAction() {
		setText(M.Export);
		setImageDescriptor(Icon.EXPORT.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		if (!(selection.getFirst() instanceof SdModelElement e))
			return false;
		elem = e;
		return true;
	}

	@Override
	public void run() {
		if (elem == null)
			return;
		var dir = elem.getContent();
		var file = SystemDynamics.getXmileFile(dir);
		if (file == null || !file.exists()) {
			MsgBox.error("Failed to export model", "No XMILE file found in: " + dir);
			return;
		}
		var target = FileChooser.forSavingFile(M.Export, file.getName());
		if (target == null)
			return;
		try {
			Files.copy(
				file.toPath(),
				target.toPath(),
				StandardCopyOption.REPLACE_EXISTING,
				StandardCopyOption.COPY_ATTRIBUTES);
		} catch (Exception e) {
			ErrorReporter.on("Failed to export system dynamics model", e);
		}
	}
}
