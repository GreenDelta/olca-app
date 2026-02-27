package org.openlca.app.navigation.actions.sd;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.action.Action;
import org.openlca.app.components.FileChooser;
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

		var file = FileChooser.openFile()
				.withTitle("Select an XMILE model file")
				.withExtensions("xml", "stmx")
				.select()
				.orElse(null);
		if (file == null)
			return;
		var model = readModel(file);
		if (model == null)
			return;
		if (!checkDuplicate(model))
			return;
		writeAndOpen(model);
	}

	private SdModel readModel(File file) {
		var res = SdModel.readFrom(file);
		if (res.isError()) {
			MsgBox.error("Failed to read model", res.error());
			return null;
		}
		var model = res.value();
		if (Strings.isBlank(model.name())) {
			model.setName("System dynamics model");
		}
		if (Strings.isBlank(model.id())) {
			model.setId(UUID.randomUUID().toString());
		}
		return model;
	}

	private boolean checkDuplicate(SdModel model) {
		var db = Database.get();
		for (var dir : SystemDynamics.getModelDirsOf(db)) {
			if (model.id().equals(dir.getName())) {
				var ok = Question.ask(
						"Duplicate model ID",
						"A model with the same ID already exists ("
								+ SystemDynamics.modelNameOf(dir)
								+ "). Import as a copy with a new ID?");
				if (!ok)
					return false;
				model.setId(UUID.randomUUID().toString());
				model.setName(model.name() + " (copy)");
				break;
			}
		}
		return true;
	}

	private void writeAndOpen(SdModel model) {
		var dirRes = SystemDynamics.createModelDir(model.id(), Database.get());
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
