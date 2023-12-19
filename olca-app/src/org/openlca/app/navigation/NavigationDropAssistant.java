package org.openlca.app.navigation;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.core.database.config.DerbyConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class NavigationDropAssistant extends CommonDropAdapterAssistant {

	@Override
	public IStatus handleDrop(
			CommonDropAdapter adapter, DropTargetEvent event, Object target) {
		if (skip(event))
			return Status.CANCEL_STATUS;
		if (!(target instanceof INavigationElement<?> targetElem))
			return Status.CANCEL_STATUS;

		if (targetElem instanceof DatabaseDirElement
				|| targetElem instanceof NavigationRoot) {
			var elements = elementsOf(event, e -> e instanceof DatabaseElement);
			return moveDatabases(elements, targetElem);
		}

		if (targetElem instanceof ModelTypeElement
				|| targetElem instanceof CategoryElement) {
			var elements = elementsOf(event,
					e -> !Objects.equals(e, targetElem)
							&& (e instanceof ModelElement || e instanceof CategoryElement));
			return moveModels(event.detail, elements, targetElem);
		}

		return Status.CANCEL_STATUS;
	}

	/**
	 * Only handle drag-and-drop events that happen within the navigator.
	 */
	private boolean skip(DropTargetEvent e) {
		if (e == null)
			return true;
		var navigator = Navigator.getInstance();
		if (navigator == null)
			return true;
		var viewer = navigator.getCommonViewer();
		if (viewer == null)
			return true;
		if (!(e.getSource() instanceof DropTarget target))
			return true;
		return target.getControl() != viewer.getTree();
	}

	/**
	 * In the navigation, we only handle drag-and-drop events that happen within
	 * the navigation. The dragged elements are then directly in the data of the
	 * drop event.
	 */
	private List<INavigationElement<?>> elementsOf(
			DropTargetEvent e, Predicate<INavigationElement<?>> filter) {
		if (!(e.data instanceof IStructuredSelection selection))
			return List.of();
		var elements = new ArrayList<INavigationElement<?>>();
		for (var o : selection) {
			if (!(o instanceof INavigationElement<?> elem))
				continue;
			if (!filter.test(elem))
				continue;
			elements.add(elem);
		}
		return elements;
	}

	private IStatus moveModels(
			int op,
			List<INavigationElement<?>> elements,
			INavigationElement<?> target
	) {
		if (!CopyPaste.canMove(elements, target))
			return Status.CANCEL_STATUS;
		boolean copy = (op & DND.DROP_COPY) == DND.DROP_COPY;
		if (copy) {
			CopyPaste.copy(elements);
		} else {
			CopyPaste.cut(elements);
		}
		CopyPaste.pasteTo(target);
		return Status.OK_STATUS;
	}

	private IStatus moveDatabases(
			List<INavigationElement<?>> elements, INavigationElement<?> target) {
		if (elements.isEmpty())
			return Status.CANCEL_STATUS;
		var path = target instanceof DatabaseDirElement dir
				? String.join("/", dir.path())
				: "";
		int moved = 0;
		for (var e : elements) {
			if (e instanceof DatabaseElement dbElem) {
				var config = dbElem.getContent();
				if (config instanceof DerbyConfig dc) {
					dc.setCategory(path);
					moved++;
				}
			}
		}
		if (moved == 0)
			return Status.CANCEL_STATUS;
		Database.saveConfig();
		Navigator.refresh();
		return Status.OK_STATUS;
	}

	@Override
	public boolean isSupportedType(TransferData data) {
		// This method is only called when the drag started outside
		// the navigation tree (?). As we currently do not support such
		// drops that come from outside the tree, we always return false
		// here.
		return false;
	}

	@Override
	public IStatus validateDrop(Object target, int operation, TransferData data) {
		if (target instanceof NavigationRoot
				|| target instanceof DatabaseDirElement
				|| target instanceof ModelTypeElement
				|| target instanceof CategoryElement)
			return Status.OK_STATUS;
		return Status.CANCEL_STATUS;
	}
}
