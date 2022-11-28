package org.openlca.app.tools.graphics.frame;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.openlca.app.tools.graphics.edit.RootEditPart;
import org.openlca.app.tools.graphics.model.Element;

import java.beans.PropertyChangeListener;

public abstract class Header extends Composite implements
		PropertyChangeListener {

	protected static Dimension DEFAULT_SIZE = new Dimension(SWT.DEFAULT, 60);

	private RootEditPart rootEditPart;
	private Element model;
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

	public void setModel(Element element) {
		if (model != null)
			deactivate();
		model = element;
		if (model != null)
			activate();
	}

	public Element getModel() {
		return model;
	}

	public void activate() {
		if (getModel() != null)
			getModel().addPropertyChangeListener(this);

		var viewer = getRootEditPart().getViewer();
		if (viewer != null) {
			disposeListener = e -> deactivate();
			viewer.getControl().addDisposeListener(disposeListener);
		}
	}

	public void deactivate() {
		if (getModel() != null)
			getModel().removePropertyChangeListener(this);

		var viewer = getRootEditPart().getViewer();
		if (viewer != null && viewer.getControl() != null)
			viewer.getControl().removeDisposeListener(disposeListener);
	}

}
