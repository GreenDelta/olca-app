/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/

package org.openlca.ui.dnd;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.modelprovider.IModelComponent;

/**
 * A extended {@link TableViewer} with drop support
 * 
 * @author Sebastian Greve
 */
public class ViewerDropComponent extends TableViewer {

	/**
	 * The {@link IDropHandler} which handles dropped {@link IModelComponent}s
	 */
	private final IDropHandler handler;

	/**
	 * The transfer type
	 */
	private final Transfer transferType = ModelComponentTransfer.getInstance();

	/**
	 * Constructor for a new ViewerDropComponent
	 * 
	 * @param parent
	 *            the parent {@link Composite}
	 * @param clazz
	 *            the class this viewer allows to be dropped
	 * @param handler
	 *            the {@link IDropHandler} of this viewer
	 * @param database
	 *            The database
	 */
	public ViewerDropComponent(final Composite parent,
			final Class<? extends IModelComponent> clazz,
			final IDropHandler handler, final IDatabase database) {
		super(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		this.handler = handler;
		if (clazz != null && handler != null) {
			final DropTarget dropTarget = new DropTarget(getTable(),
					DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
			dropTarget.setTransfer(new Transfer[] { transferType });
			dropTarget.addDropListener(new DropTargetAdapter() {

				@Override
				public void dragEnter(final DropTargetEvent event) {
					super.dragEnter(event);
				}

				@Override
				public void drop(final DropTargetEvent event) {
					if (transferType.isSupportedType(event.currentDataType)
							&& event.data != null) {
						final Object[] data = (Object[]) event.data;
						if (data[data.length - 1] instanceof String
								&& data[data.length - 1].equals(database
										.getUrl())) {
							if (data[0].getClass() == clazz) {
								final IModelComponent[] copy = new IModelComponent[data.length - 1];
								for (int i = 0; i < copy.length; i++) {
									copy[i] = (IModelComponent) data[i];
								}
								setData(copy);
							}
						}
					}
				}

			});
		}
	}

	/**
	 * Sets the data of the component
	 * 
	 * @param data
	 *            The data to set
	 */
	public void setData(final Object data) {
		if (data != null && data instanceof IModelComponent[]) {
			handler.handleDrop((IModelComponent[]) data);
		}
	}

	@Override
	public void setData(final String key, final Object value) {
		setData(value);
	}

}
