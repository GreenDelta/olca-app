package org.openlca.app.tools.graphics.edit;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.editparts.FreeformGraphicalRootEditPart;
import org.eclipse.gef.editparts.GuideLayer;
import org.eclipse.gef.editparts.ViewportAutoexposeHelper;
import org.openlca.app.tools.graphics.zoom.ZoomManager;

/**
 * <i>This class is a copy of
 * {@link org.eclipse.gef.editparts.ScalableFreeformRootEditPart} to have the
 * ability to use our own ZoomManager.</i>
 */
public class RootEditPart extends FreeformGraphicalRootEditPart {

	public static final Insets MARGIN_PADDING = new Insets(1100, 1650, 0, 0);

	private ScalableFreeformLayeredPane scaledLayers;
	private final ZoomManager zoomManager;

	/**
	 * Constructor for the RootEditPart
	 */
	public RootEditPart(EditPartViewer viewer) {
		zoomManager = createZoomManager(viewer,
			(ScalableFigure) getScaledLayers(), ((Viewport) getFigure()));
	}

	/**
	 * Responsible for creating a {@link ZoomManager} to be used by this
	 * {@link RootEditPart}.
	 *
	 * @return A new {@link ZoomManager} bound to the given
	 *         {@link ScalableFigure} and {@link Viewport}.
	 * @since 3.10
	 */
	protected ZoomManager createZoomManager(EditPartViewer viewer,
																					ScalableFigure scalableFigure, Viewport viewport) {
		return new ZoomManager(viewer, scalableFigure, viewport);
	}

	@Override
	protected void createLayers(LayeredPane layeredPane) {
		layeredPane.add(getScaledLayers(), SCALABLE_LAYERS);
		layeredPane.add(new FreeformLayer(), HANDLE_LAYER);
		layeredPane.add(new FeedbackLayer(), FEEDBACK_LAYER);
		layeredPane.add(new GuideLayer(), GUIDE_LAYER);
	}

	/**
	 * Creates a layered pane and the layers that should be scaled.
	 *
	 * @return a new freeform layered pane containing the scalable layers
	 */
	protected ScalableFreeformLayeredPane createScaledLayers() {
		ScalableFreeformLayeredPane layers = new ScalableFreeformLayeredPane();
		layers.add(createGridLayer(), GRID_LAYER);
		layers.add(getPrintableLayers(), PRINTABLE_LAYERS);
		layers.add(new FeedbackLayer(), SCALED_FEEDBACK_LAYER);
		layers.setBorder(new MarginBorder(MARGIN_PADDING));
		return layers;
	}

	@Override
	public IFigure getLayer(Object key) {
		IFigure layer = scaledLayers.getLayer(key);
		if (layer != null)
			return layer;
		return super.getLayer(key);
	}

	/**
	 * Returns the scalable layers of this EditPart
	 *
	 * @return LayeredPane
	 */
	protected LayeredPane getScaledLayers() {
		if (scaledLayers == null)
			scaledLayers = createScaledLayers();
		return scaledLayers;
	}

	/**
	 * Override the original to be able to inverse the addition of the connection
	 * and primary layer.
	 */
	@Override
	protected LayeredPane createPrintableLayers() {
		FreeformLayeredPane layeredPane = new FreeformLayeredPane();
		layeredPane.add(new ConnectionLayer(), CONNECTION_LAYER);
		layeredPane.add(new FreeformLayer(), PRIMARY_LAYER);
		return layeredPane;
	}

	/**
	 * Returns the zoomManager.
	 *
	 * @return ZoomManager
	 */
	public ZoomManager getZoomManager() {
		return zoomManager;
	}

	@Override
	protected void register() {
		super.register();
		getViewer().setProperty(ZoomManager.class.toString(),
			getZoomManager());
	}

	@Override
	protected void unregister() {
		super.unregister();
		getViewer().setProperty(ZoomManager.class.toString(), null);
	}

	static class FeedbackLayer extends FreeformLayer {
		FeedbackLayer() {
			setEnabled(false);
		}
	}

	@Override
	public Object getAdapter(Class adapter) {
		if (adapter == AutoexposeHelper.class)
			return new ViewportAutoexposeHelper(this, new Insets(50));
		return super.getAdapter(adapter);
	}

}
