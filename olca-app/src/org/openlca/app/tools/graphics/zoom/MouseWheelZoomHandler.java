package org.openlca.app.tools.graphics.zoom;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.swt.widgets.Event;

/**
 * A copy of {@link org.eclipse.gef.MouseWheelZoomHandler} using our
 * ZoomManager.
 */
public final class MouseWheelZoomHandler implements MouseWheelHandler {

	/**
	 * The Singleton
	 */
	public static final MouseWheelHandler SINGLETON =
		new MouseWheelZoomHandler();

	private MouseWheelZoomHandler() {
	}

	@Override
	public void handleMouseWheel(Event event, EditPartViewer viewer) {
		ZoomManager zoomMgr = (ZoomManager) viewer
			.getProperty(ZoomManager.class.toString());
		if (zoomMgr != null) {
			if (event.count > 0)
				zoomMgr.zoomIn();
			else
				zoomMgr.zoomOut();
			event.doit = false;
		}
	}

}
