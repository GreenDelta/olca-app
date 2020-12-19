package org.openlca.app.navigation.actions.scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.MsgBox;
import org.openlca.app.util.Question;

public class DeleteScriptAction extends Action implements INavigationAction {

	private final List<ScriptElement> elements = new ArrayList<>();

	@Override
	public boolean accept(List<INavigationElement<?>> elems) {
		elements.clear();
		for (var elem : elems) {
			if (elem instanceof ScriptElement) {
				this.elements.add((ScriptElement) elem);
			}
		}
		return !elements.isEmpty();
	}

	@Override
	public String getText() {
		return M.Delete;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.DELETE.descriptor();
	}

	@Override
	public void run() {
		if (elements.isEmpty())
			return;
		var deleted = new AtomicBoolean(false);
		elements.stream()
				.map(e -> e.getContent())
				.filter(File::exists)
				.filter(file -> file.isDirectory()
						? Question.ask("Delete all files in folder",
						"Do you want to delete all files in "
								+ file.getName() + "?")
						: Question.ask("Delete file",
						"Do you want to delete the file "
								+ file.getName() + "?"))
				.forEach(file -> {
					try {
						FileUtils.forceDelete(file);
						deleted.set(true);
					} catch (Exception e) {
						MsgBox.error("Failed to delete file: " + file.getName());
					}
				});

		if (deleted.get()) {
			Navigator.refresh();
		}
	}
}
