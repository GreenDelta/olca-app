package org.openlca.app.navigation.actions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.navigation.DatabaseElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.validation.ValidationView;

public class ValidateAction extends Action implements INavigationAction {

	private final Set<INavigationElement<?>> selection = new HashSet<>();
	private final boolean isDbAction; 
	// used to have a different order for non database elements
	
	public static ValidateAction forDatabase() {
		return new ValidateAction(true);
	}

	public static ValidateAction forModel() {
		return new ValidateAction(false);
	}

	private ValidateAction(boolean onlyIfDbElement) {
		setText(M.Validate);
		setImageDescriptor(Icon.VALIDATE.descriptor());
		this.isDbAction = onlyIfDbElement;
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		selection.clear();
		if (element instanceof DatabaseElement) {
			if (!isDbAction) 
				return false;
			DatabaseElement e = (DatabaseElement) element;
			IDatabaseConfiguration config = e.getContent();
			if (!Database.isActive(config))
				return false;
		} else if (isDbAction) {
			return false;
		}
		selection.add(element);
		return !selection.isEmpty();
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		selection.clear();
		for (INavigationElement<?> element : elements) {
			if (!(element instanceof DatabaseElement))
				continue;
			DatabaseElement e = (DatabaseElement) element;
			IDatabaseConfiguration config = e.getContent();
			if (!Database.isActive(config))
				return false;
		}
		selection.addAll(elements);
		return !selection.isEmpty();
	}

	@Override
	public void run() {
		ValidationView.validate(selection);
	}

}
