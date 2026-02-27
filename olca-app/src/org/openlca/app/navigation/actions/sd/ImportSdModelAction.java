package org.openlca.app.navigation.actions.sd;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.openlca.app.db.Database;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.navigation.elements.SdRootElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;
import org.openlca.app.util.SystemDynamics;
import org.openlca.app.util.UI;
import org.openlca.commons.Strings;
import org.openlca.sd.model.SdModel;

public class ImportSdModelAction extends Action implements INavigationAction {

	public ImportSdModelAction() {
		setText("Import system dynamics model");
		setImageDescriptor(Icon.SD.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.getFirst();
		return first instanceof SdRootElement
				|| first instanceof SdModelElement;
	}

	@Override
	public void run() {
		var db = Database.get();
		if (db == null) {
			MsgBox.error("No database", "No database is currently active.");
			return;
		}

		// open file dialog
		var dialog = new FileDialog(UI.shell(), SWT.OPEN);
		dialog.setText("Select an XMILE model file");
		dialog.setFilterExtensions(new String[]{"*.xml;*.stmx", "*.*"});
		dialog.setFilterNames(new String[]{"XMILE files (*.xml, *.stmx)", "All files"});
		var path = dialog.open();
		if (path == null)
			return;

		var file = new File(path);
		if (!file.exists() || !file.isFile()) {
			MsgBox.error("Invalid file", "The selected file does not exist.");
			return;
		}

		// read & validate the model
		var res = SdModel.readFrom(file);
		if (res.isError()) {
			MsgBox.error("Failed to read model", res.error());
			return;
		}
		var model = res.value();

		// ensure the model has a name
		if (Strings.isBlank(model.name())) {
			model.setName("System dynamics model");
		}

		// ensure the model has a UUID
		if (Strings.isBlank(model.id())) {
			model.setId(UUID.randomUUID().toString());
		}

		// check for duplicate UUID
		var uuid = model.id();
		for (var dir : SystemDynamics.getModelDirsOf(db)) {
			if (uuid.equals(dir.getName())) {
				var ok = Question.ask(
						"Duplicate model ID",
						"A model with the same ID already exists ("
								+ SystemDynamics.modelNameOf(dir)
								+ "). Import as a copy with a new ID?");
				if (!ok)
					return;
				model.setId(UUID.randomUUID().toString());
				model.setName(model.name() + " (copy)");
				break;
			}
		}

		// create directory and write the model
		var dirRes = SystemDynamics.createModelDir(model.id(), db);
		if (dirRes.isError()) {
			MsgBox.error("Failed to create model folder", dirRes.error());
			return;
		}
		var modelDir = dirRes.value();
		var targetName = SystemDynamics.sanitizeName(model.name()) + ".xml";
		var targetFile = new File(modelDir, targetName);
		var err = model.writeTo(targetFile);
		if (err.isError()) {
			MsgBox.error("Failed to write model", err.error());
			return;
		}

		SdModelEditor.open(modelDir);
		Navigator.refresh();
	}
}
