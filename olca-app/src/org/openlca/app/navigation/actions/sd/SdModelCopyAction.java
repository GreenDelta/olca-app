package org.openlca.app.navigation.actions.sd;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.SystemDynamics;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.SdModel;

import java.util.List;
import java.util.UUID;

public class SdModelCopyAction extends Action implements INavigationAction {

	private SdModelElement elem;

	public SdModelCopyAction() {
		setText(M.Copy);
		setImageDescriptor(Icon.COPY.descriptor());
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
		var db = Database.get();
		if (db == null) return;

		var file = SystemDynamics.getXmileFile(elem.getContent());
		if (file == null) return;
		var modelRes = SdModel.readFrom(file);
		if (modelRes.isError()) {
			MsgBox.error("Failed to read model file", modelRes.error());
			return;
		}

		var model = modelRes.value();
		var dialog = new InputDialog(
			UI.shell(),
			"Copy model",
			"New model name",
			model.name() + " (copy)",
			v -> Strings.isBlank(v)
				? "The name cannot be empty"
				: null);
		if (dialog.open() != InputDialog.OK) {
			return;
		}
		model.setName(dialog.getValue());
		model.setId(UUID.randomUUID().toString());

		var saveRes = SystemDynamics.saveModel(model, db);
		if (saveRes.isError()) {
			MsgBox.error("Failed to copy model", saveRes.error());
			return;
		}
		Navigator.refresh();
	}
}
