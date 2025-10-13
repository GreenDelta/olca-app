package org.openlca.app.navigation.actions.sd;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.SdModelElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.util.Question;
import org.openlca.util.Dirs;

public class DeleteSdModelAction extends Action implements INavigationAction {

	private List<SdModelElement> elems;

	public DeleteSdModelAction() {
		setText(M.Delete);
		setImageDescriptor(Icon.DELETE.descriptor());
	}

	@Override
	public boolean accept(List<INavigationElement<?>> selection) {
		if (selection == null)
			return false;
		var xs = selection.stream()
				.filter(SdModelElement.class::isInstance)
				.map(SdModelElement.class::cast)
				.toList();
		if (xs.isEmpty())
			return false;
		elems = xs;
		return true;
	}

	@Override
	public void run() {
		if (elems == null || elems.isEmpty())
			return;
		var b = Question.ask(
				"Delete model(s)?",
				"Do you really want to delete the selected model(s)?");
		if (!b)
			return;
		try {
			for (var e : elems) {
				Dirs.delete(e.getContent());
			}
		} catch (Exception e) {
			ErrorReporter.on("Failed to delete SD model(s)", e);
		} finally {
			Navigator.refresh();
		}
	}
}
