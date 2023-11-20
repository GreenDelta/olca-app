package org.openlca.app.navigation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.rcp.RcpActivator;

import com.google.common.base.Objects;

public class NavigationDropAssistant extends CommonDropAdapterAssistant {

	@Override
	public IStatus handleDrop(
			CommonDropAdapter adapter, DropTargetEvent event, Object target
	) {
		var navigator = Navigator.getInstance();
		if (navigator == null)
			return null;
		if (!(target instanceof INavigationElement<?> element))
			return null;
		if (!(event.getSource() instanceof DropTarget dropTarget))
			return null;
		if (dropTarget.getControl() == navigator.getCommonViewer().getTree()) {
			doIt(event, element);
		}
		return null;
	}

	private void doIt(DropTargetEvent event, INavigationElement<?> targetElement) {
		var elements = getElements(event, targetElement);
		if (!CopyPaste.canMove(elements, targetElement))
			return;
		boolean copy = (event.detail & DND.DROP_COPY) == DND.DROP_COPY;
		if (copy) {
			CopyPaste.copy(elements);
		} else {
			CopyPaste.cut(elements);
		}
		CopyPaste.pasteTo(targetElement);
	}

	private List<INavigationElement<?>> getElements(
			DropTargetEvent event, INavigationElement<?> target) {
		if (!(event.data instanceof IStructuredSelection selection))
			return List.of();
		var elements = new ArrayList<INavigationElement<?>>();
		for (var o : selection) {
			if (!(o instanceof ModelElement || o instanceof CategoryElement))
				continue;
			if (Objects.equal(o, target))
				continue;
			elements.add((INavigationElement<?>) o);
		}
		return elements;
	}

	@Override
	public boolean isSupportedType(TransferData data) {
		return true;
	}

	@Override
	public IStatus validateDrop(Object target, int operation, TransferData data) {
		if (target instanceof CategoryElement || target instanceof ModelTypeElement)
			return new Status(IStatus.OK, RcpActivator.PLUGIN_ID, "");
		return null;
	}

}
