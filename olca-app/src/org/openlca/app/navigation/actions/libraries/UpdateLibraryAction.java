package org.openlca.app.navigation.actions.libraries;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.M;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.Question;

public class UpdateLibraryAction extends Action implements INavigationAction {

	private LibraryElement element;

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection.size() != 1)
			return false;
		var first = selection.get(0);
		if (first instanceof LibraryElement) {
			this.element = (LibraryElement) first;
			if (this.element.getContent() == null)
				return false;
			return this.element.getDatabase().isPresent();
		}
		return false;
	}

	@Override
	public String getText() {
		return M.UpdateLibraryExperimental;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.UPDATE.descriptor();
	}

	@Override
	public void run() {
		if (element == null)
			return;
		if (!Question.ask(M.UpdatingLibraryWarning,
				M.ActionMightBreakDatabase + "\r\n\r\n"
						+ "* " + M.UnitsUsedInExchangeRemoved + "\r\n"
						+ "* " + M.FlowsLinkedToProductSystemRemoved + "\r\n"
						+ "* " + M.ParametersUsedInFormulaRemoved + "\r\n\r\n"
						+ M.RunDatabaseValidationAfterReplacingLibrary + "\r\n\r\n"
						+ M.DoYouWantToContinue))
			return;
		var replaceWith = AddLibraryAction.askForLibrary();
		if (replaceWith == null)
			return;
		var toReplace = element.getContent();
		LibraryActions.unmount(toReplace,
				() -> LibraryActions.mount(replaceWith));
	}

}
