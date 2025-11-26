package org.openlca.app.editors.sd.editor.graph;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;

/// Key handler with arrow key scrolling support.
class DefaultKeyHandler extends KeyHandler {

	private final GraphicalViewer viewer;

	DefaultKeyHandler(GraphicalViewer viewer) {
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

	private boolean acceptScroll(KeyEvent event) {
		return event.keyCode == SWT.ARROW_DOWN
			|| event.keyCode == SWT.ARROW_LEFT
			|| event.keyCode == SWT.ARROW_RIGHT
			|| event.keyCode == SWT.ARROW_UP;
	}

	private void scrollViewer(KeyEvent event) {
		if (!(viewer.getControl() instanceof FigureCanvas canvas))
			return;
		var loc = canvas.getViewport().getViewLocation();
		var area = canvas.getViewport()
			.getClientArea(Rectangle.SINGLETON)
			.scale(.1);
		switch (event.keyCode) {
			case SWT.ARROW_DOWN:
				canvas.scrollToY(loc.y + area.height);
				break;
			case SWT.ARROW_UP:
				canvas.scrollToY(loc.y - area.height);
				break;
			case SWT.ARROW_LEFT:
				if (isViewerMirrored())
					canvas.scrollToX(loc.x + area.width);
				else
					canvas.scrollToX(loc.x - area.width);
				break;
			case SWT.ARROW_RIGHT:
				if (isViewerMirrored())
					canvas.scrollToX(loc.x - area.width);
				else
					canvas.scrollToX(loc.x + area.width);
				break;
		}
	}

	private boolean isViewerMirrored() {
		return (viewer.getControl().getStyle() & SWT.MIRRORED) != 0;
	}
}
