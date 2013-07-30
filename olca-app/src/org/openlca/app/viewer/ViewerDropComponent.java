package org.openlca.app.viewer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.dnd.IModelDropHandler;
import org.openlca.app.dnd.ModelTransfer;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

/**
 * A table viewer with drop support for arrays of base descriptor instances (see
 * {@link ModelTransfer}.
 */
public class ViewerDropComponent extends TableViewer {

	private IModelDropHandler handler;
	private Transfer transferType = ModelTransfer.getInstance();

	public ViewerDropComponent(Composite parent, ModelType type,
			IModelDropHandler handler) {
		super(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		this.handler = handler;
		DropTarget dropTarget = new DropTarget(getTable(), DND.DROP_COPY
				| DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		dropTarget.addDropListener(new DropDispatch());
	}

	private class DropDispatch extends DropTargetAdapter {

		@Override
		public void drop(DropTargetEvent event) {
			if (!transferType.isSupportedType(event.currentDataType)
					|| !(event.data instanceof Object[]))
				return;
			Object[] data = (Object[]) event.data;
			List<BaseDescriptor> dropped = new ArrayList<>();
			for (Object o : data) {
				if (o instanceof BaseDescriptor)
					dropped.add((BaseDescriptor) o);
			}
			handler.handleDrop(dropped);
		}
	}

}
