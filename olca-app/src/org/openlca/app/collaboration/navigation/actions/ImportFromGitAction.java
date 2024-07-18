package org.openlca.app.collaboration.navigation.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.collaboration.dialogs.ConnectDialog;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.rcp.images.Icon;

public class ImportFromGitAction extends Action implements INavigationAction {

	private final CloneActionType type;

	private ImportFromGitAction(CloneActionType type) {
		this.type = type;
	}

	public static ImportFromGitAction forImportMenu() {
		return new ImportFromGitAction(CloneActionType.IMPORT);
	}

	public static ImportFromGitAction forRootMenu() {
		return new ImportFromGitAction(CloneActionType.ROOT);
	}

	@Override
	public String getText() {
		return switch (type) {
			case ROOT -> M.ImportFromGitDots;
			case IMPORT -> M.FromGitDots;
		};
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.CLONE.descriptor();
	}

	@Override
	public void run() {
		var dialog = new ConnectDialog().withPassword();
		if (dialog.open() == ConnectDialog.CANCEL)
			return;
		var url = dialog.url();
		Clone.of(url, dialog.user(), dialog.password());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() > 1)
			return false;
		if (selection.size() == 0)
			return type == CloneActionType.ROOT;
		var first = selection.get(0);
		if (!(first instanceof DatabaseElement dbElem))
			return false;
		return !Database.isActive(dbElem.getContent()) && type == CloneActionType.ROOT;
	}

	private enum CloneActionType {

		ROOT, IMPORT;

	}

}
