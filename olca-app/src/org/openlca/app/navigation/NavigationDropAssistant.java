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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;
import org.openlca.app.rcp.RcpActivator;

import com.google.common.base.Objects;

/**
 * Extension of the {@link CommonDropAdapterAssistant} to support drop
 * assistance for the common viewer of the applications navigator
 */

public class NavigationDropAssistant extends CommonDropAdapterAssistant {

	@Override
	public IStatus handleDrop(CommonDropAdapter dropAdapter,
			DropTargetEvent dropTargetEvent, Object target) {
		Navigator navigator = (Navigator) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(Navigator.ID);
		DropTarget dropTarget = (DropTarget) dropTargetEvent.getSource();
		INavigationElement<?> targetElement = (INavigationElement<?>) target;
		if (dropTarget.getControl() == navigator.getCommonViewer().getTree())
			doIt(dropTargetEvent, targetElement);
		return null;
	}

	private void doIt(DropTargetEvent event, INavigationElement<?> targetElement) {
		List<INavigationElement<?>> elements = new ArrayList<>();
		IStructuredSelection selection = (IStructuredSelection) event.data;
		for (Object o : selection.toArray())
			if ((o instanceof ModelElement || o instanceof CategoryElement)
					&& !Objects.equal(o, targetElement))
				elements.add((INavigationElement<?>) o);
		if (CopyPaste.canMove(elements, targetElement)) {
			if ((event.detail & DND.DROP_COPY) == DND.DROP_COPY)
				CopyPaste.copy(elements);
			else
				CopyPaste.cut(elements);
			CopyPaste.pasteTo(targetElement);
		}
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return true;
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
			TransferData transferType) {
		IStatus status = null;
		if (target instanceof CategoryElement
				|| target instanceof ModelTypeElement)
			status = new Status(IStatus.OK, RcpActivator.PLUGIN_ID, "");
		return status;
	}

}
