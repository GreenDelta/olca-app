package org.openlca.app.components.graphics.frame;

import java.beans.PropertyChangeListener;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.components.graphics.edit.RootEditPart;
import org.openlca.app.components.graphics.model.PropertyNotifier;

public abstract class Header extends Composite implements
		PropertyChangeListener {

	protected static Dimension DEFAULT_SIZE = new Dimension(SWT.DEFAULT, 50);

	private RootEditPart rootEditPart;
	private PropertyNotifier model;
	private DisposeListener disposeListener;

	public Header(Composite parent, int style) {
		super(parent, style);
	}

	public void setRootEditPart(RootEditPart part) {
		rootEditPart = part;
	}

	public RootEditPart getRootEditPart() {
		return rootEditPart;
	}

	public abstract void initialize();

	public void setModel(PropertyNotifier element) {
		if (model != null)
			deactivate();
		model = element;
		if (model != null)
			activate();
	}

	public PropertyNotifier getModel() {
		return model;
	}

	public void activate() {
		if (getModel() != null)
			getModel().addListener(this);

		var viewer = getRootEditPart().getViewer();
		if (viewer != null) {
			disposeListener = e -> deactivate();
			viewer.getControl().addDisposeListener(disposeListener);
		}
	}

	public void deactivate() {
		if (getModel() != null)
			getModel().removeListener(this);

		var viewer = getRootEditPart().getViewer();
		if (viewer != null && viewer.getControl() != null)
			viewer.getControl().removeDisposeListener(disposeListener);
	}

}
