package org.openlca.app.navigation.actions.cloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndex;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.actions.INavigationAction;

public class ToggleTrackingAction extends Action implements INavigationAction {

	private final boolean setTracked;
	private DiffIndex index;
	private List<String> elementRefIds;

	public static ToggleTrackingAction track() {
		return new ToggleTrackingAction(true);
	}

	public static ToggleTrackingAction untrack() {
		return new ToggleTrackingAction(false);
	}

	private ToggleTrackingAction(boolean setTracked) {
		this.setTracked = setTracked;
	}

	@Override
	public String getText() {
		return setTracked ? "#Track" : "#Untrack";
	}

	@Override
	public void run() {
		for (String refId : elementRefIds) {
			Diff diff = index.get(refId);
			if (diff == null)
				continue;
			if (diff.type == DiffType.UNTRACKED && !setTracked)
				continue;
			if (diff.type != DiffType.UNTRACKED && setTracked)
				continue;
			if (setTracked) {
				index.retrack(refId);
			} else {
				index.untrack(refId);
			}
		}
		Navigator.refresh();
	}

	@Override
	public boolean accept(INavigationElement<?> element) {
		return accept(Arrays.asList(element));
	}

	@Override
	public boolean accept(List<INavigationElement<?>> elements) {
		this.elementRefIds = new ArrayList<>();
		index = Database.getDiffIndex();
		if (index == null)
			return false;
		Set<INavigationElement<?>> deepSelection = Navigator.collect(elements,
				e -> e instanceof ModelElement ? e : null);
		for (INavigationElement<?> e : deepSelection) {
			String refId = ((ModelElement) e).getContent().refId;
			this.elementRefIds.add(refId);
		}
		return true;
	}

}
