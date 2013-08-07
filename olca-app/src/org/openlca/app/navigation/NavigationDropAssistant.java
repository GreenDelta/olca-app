/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
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
import org.openlca.app.plugin.Activator;
import org.openlca.app.util.CopyPaste;

/**
 * Extension of the {@link CommonDropAdapterAssistant} to support drop
 * assistance for the common viewer of the applications navigator
 */

public class NavigationDropAssistant extends CommonDropAdapterAssistant {

	private int operation;

	public NavigationDropAssistant() {
		// nothing to initialize
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter dropAdapter,
			DropTargetEvent dropTargetEvent, Object target) {
		Navigator navigator = (Navigator) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage()
				.findView(Navigator.ID);
		DropTarget dropTarget = (DropTarget) dropTargetEvent.getSource();

		INavigationElement<?> targetElement = (INavigationElement<?>) target;
		if (dropTarget.getControl() == navigator.getCommonViewer().getTree()) {
			List<INavigationElement<?>> elements = new ArrayList<>();
			IStructuredSelection selection = (IStructuredSelection) dropTargetEvent.data;
			for (Object o : selection.toArray())
				if (o instanceof ModelElement || o instanceof CategoryElement)
					elements.add((INavigationElement<?>) o);
			if (CopyPaste.canMove(elements, targetElement)) {
				if (operation == DND.DROP_COPY)
					CopyPaste.copy(elements);
				else
					CopyPaste.cut(elements);
				CopyPaste.pasteTo(targetElement);
			}
		}
		return null;
	}

	@Override
	public boolean isSupportedType(TransferData aTransferType) {
		return true;
	}

	@Override
	public IStatus validateDrop(Object target, int operation,
			TransferData transferType) {
		this.operation = operation;
		IStatus status = null;

		if (target instanceof CategoryElement
				|| target instanceof ModelTypeElement)
			status = new Status(IStatus.OK, Activator.PLUGIN_ID, "");
		return status;
	}

}
