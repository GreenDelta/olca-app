package org.openlca.app.navigation.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.rcp.images.Icon;

class OpenModelAction extends Action implements INavigationAction {

	private List<ModelElement> elements;

	@Override
	public boolean accept(INavigationElement<?> element) {
		if (!(element instanceof ModelElement))
			return false;
		elements = Collections.singletonList((ModelElement) element);
		return true;
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		List<ModelElement> models = new ArrayList<>();
		for (INavigationElement<?> element : elements)
			if (!(element instanceof ModelElement))
				return false;
			else
				models.add((ModelElement) element);
		this.elements = models;
		return true;
	}

	@Override
	public String getText() {
		return M.Open;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return Icon.FOLDER_OPEN.descriptor();
	}

	@Override
	public void run() {
		for (ModelElement element : elements)
			App.openEditor(element.getContent());
	}

}
