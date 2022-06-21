package org.openlca.app.editors.graphical.zoom;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.swt.widgets.Event;

/**
 * A copy of {@link org.eclipse.gef.MouseWheelZoomHandler} using our
 * GraphZoomManager.
 */
public final class GraphMouseWheelZoomHandler implements MouseWheelHandler {

	/**
	 * The Singleton
	 */
	public static final MouseWheelHandler SINGLETON =
		new GraphMouseWheelZoomHandler();

	private GraphMouseWheelZoomHandler() {
	}

	@Override
	public void handleMouseWheel(Event event, EditPartViewer viewer) {
		GraphZoomManager zoomMgr = (GraphZoomManager) viewer
			.getProperty(GraphZoomManager.class.toString());
		if (zoomMgr != null) {
			if (event.count > 0)
				zoomMgr.zoomIn();
			else
				zoomMgr.zoomOut();
			event.doit = false;
		}
	}

}
