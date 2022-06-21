package org.openlca.app.editors.graphical;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

public class GraphKeyHandler extends KeyHandler {

	private final GraphicalViewer viewer;

	public GraphKeyHandler(GraphicalViewer viewer) {
		this.viewer = viewer;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (acceptScroll(event)) {
			scrollViewer(event);
			return true;
		}
		return super.keyPressed(event);
	}

	boolean acceptScroll(KeyEvent event) {
		return ((event.keyCode == SWT.ARROW_DOWN || event.keyCode == SWT.ARROW_LEFT
			|| event.keyCode == SWT.ARROW_RIGHT || event.keyCode == SWT.ARROW_UP));
	}

	void scrollViewer(KeyEvent event) {
		if (!(getViewer().getControl() instanceof FigureCanvas figCanvas))
			return;
		Point loc = figCanvas.getViewport().getViewLocation();
		Rectangle area = figCanvas.getViewport()
			.getClientArea(Rectangle.SINGLETON).scale(.1);
		switch (event.keyCode) {
			case SWT.ARROW_DOWN:
				figCanvas.scrollToY(loc.y + area.height);
				break;
			case SWT.ARROW_UP:
				figCanvas.scrollToY(loc.y - area.height);
				break;
			case SWT.ARROW_LEFT:
				if (isViewerMirrored())
					figCanvas.scrollToX(loc.x + area.width);
				else
					figCanvas.scrollToX(loc.x - area.width);
				break;
			case SWT.ARROW_RIGHT:
				if (isViewerMirrored())
					figCanvas.scrollToX(loc.x - area.width);
				else
					figCanvas.scrollToX(loc.x + area.width);
		}
	}

	/**
	 * @return <code>true</code> if the viewer is mirrored
	 */
	protected boolean isViewerMirrored() {
		return (viewer.getControl().getStyle() & SWT.MIRRORED) != 0;
	}

	/**
	 * Returns the viewer on which this key handler was created.
	 *
	 * @return the viewer
	 */
	protected GraphicalViewer getViewer() {
		return viewer;
	}

}
