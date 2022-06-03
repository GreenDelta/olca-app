package org.openlca.app.navigation.actions.libraries;


import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.forms.FormDialog;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;
import org.openlca.core.library.MountAction;
import org.openlca.core.library.PreMountCheck;

class MountDialog extends FormDialog  {

	private final Map<Library, MountAction> actions = new HashMap<>();
	private final Library library;
	private final PreMountCheck.Result checkResult;

	static void show(Library library, PreMountCheck.Result checkResult) {
		if (checkResult.isError()) {
			ErrorReporter.on(
				"Failed to check library: " + library,
				checkResult.error());
			return;
		}
		if (checkResult.isEmpty()) {
			MsgBox.info("No libraries to add", "No libraries that can be added");
			return;
		}

	}

	private MountDialog(Library library, PreMountCheck.Result checkResult) {
		super(UI.shell());
		this.library = library;
		this.checkResult = checkResult;
		checkResult.getStates().forEach(pair -> {
			var lib = pair.first;
			var state = pair.second;
			if (state != null) {
				actions.put(lib, state.defaultAction());
			}
		});
	}

}
