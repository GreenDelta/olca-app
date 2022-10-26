package org.openlca.app.tools.graphics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ViewportAwareConnectionLayerClippingStrategy;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchSizeAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.tools.graphics.actions.ActionIds;
import org.openlca.app.tools.graphics.actions.FocusAction;
import org.openlca.app.tools.graphics.actions.OpenMiniatureViewAction;
import org.openlca.app.tools.graphics.actions.ZoomInAction;
import org.openlca.app.tools.graphics.actions.ZoomOutAction;
import org.openlca.app.tools.graphics.edit.RootEditPart;
import org.openlca.app.tools.graphics.model.BaseComponent;
import org.openlca.app.tools.graphics.zoom.MouseWheelZoomHandler;
import org.openlca.app.tools.graphics.zoom.ZoomManager;

import java.util.EventObject;
import java.util.List;

abstract public class BasicGraphicalEditor extends GraphicalEditor {

	private org.eclipse.gef.KeyHandler sharedKeyHandler;

	// Set zoom levels from 0.1 to 3.0 with an incrementing factor of 5%.
	private static final int ZOOM_LEVELS_LENGTH =
			(int) Math.ceil(Math.log(3.0/0.1) / Math.log(1.05));
	public static final double[] ZOOM_LEVELS = new double[ZOOM_LEVELS_LENGTH];
	static {
		for (int i = 0; i < ZOOM_LEVELS_LENGTH; i++) {
			ZOOM_LEVELS[i] = Math.pow(1.05, i) * 0.1;
		}
	}

	public boolean wasFocused = false;
	private BaseComponent model;

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();

		// TODO (francois) Implement a PanningSelectionTool without pressing SpaceBar and
		//  Selection while pressing Ctrl.
		viewer.getEditDomain().setActiveTool(new PanningSelectionTool());
		viewer.setContents(getModel());
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();

		var root = new RootEditPart(viewer);

		// set clipping strategy for connection layer
		ConnectionLayer connectionLayer = (ConnectionLayer) root
				.getLayer(LayerConstants.CONNECTION_LAYER);
		connectionLayer
				.setClippingStrategy(new ViewportAwareConnectionLayerClippingStrategy(
						connectionLayer));

		var zoom = root.getZoomManager();
		zoom.setZoomLevels(ZOOM_LEVELS);
		var zoomStrings = new String[] { ZoomManager.FIT_ALL,
				ZoomManager.FIT_HEIGHT, ZoomManager.FIT_WIDTH };
		root.getZoomManager().setZoomLevelContributions(List.of(zoomStrings));
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		var zoomIn = new ZoomInAction(root.getZoomManager());
		var zoomOut = new ZoomOutAction(root.getZoomManager());
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);

		viewer.setRootEditPart(root);
		viewer.setKeyHandler(createGraphKeyHandler());

		loadProperties();
		loadConfig();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();
		var selectionActions = getSelectionActions();
		IAction action;

		action = new MatchSizeAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new MatchWidthAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new MatchHeightAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new FocusAction(this);
		registry.registerAction(action);

		action = new OpenMiniatureViewAction(this);
		registry.registerAction(action);
	}

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	protected org.eclipse.gef.KeyHandler createGraphKeyHandler() {
		var keyHandler = new KeyHandler(getGraphicalViewer());
		keyHandler.setParent(getCommonKeyHandler());

		var registry = getActionRegistry();
		keyHandler.put(
				KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				registry.getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(
				KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				registry.getAction(GEFActionConstants.ZOOM_OUT));
		keyHandler.put(
				KeyStroke.getPressed('+', 43, SWT.MOD1),
				registry.getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(
				KeyStroke.getPressed('-', 45, SWT.MOD1),
				registry.getAction(GEFActionConstants.ZOOM_OUT));
		return keyHandler;
	}

	/**
	 * Returns the KeyHandler with common bindings for both the Outline (if it
	 * exists) and Graphical Views. For example, delete is a common action.
	 */
	protected org.eclipse.gef.KeyHandler getCommonKeyHandler() {
		if (sharedKeyHandler == null) {
			sharedKeyHandler = new org.eclipse.gef.KeyHandler();
			var registry = getActionRegistry();
			sharedKeyHandler.put(
					KeyStroke.getPressed(SWT.DEL, 127, 0),
					registry.getAction(ActionFactory.DELETE.getId()));
		}
		return sharedKeyHandler;
	}

	public BaseComponent getModel() {
		return model;
	}

	public void setModel(BaseComponent model) {
		this.model = model;
	}

	public RootEditPart getRootEditPart() {
		return (RootEditPart) getGraphicalViewer().getRootEditPart();
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	protected abstract void loadConfig();

	protected void loadProperties() {
		// Zoom
		var manager = getZoomManager();
		if (manager != null)
			manager.setZoom(getModel().getZoom());
		getGraphicalViewer().setProperty(
				MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);
	}

	@Override
	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return getZoomManager();

		return super.getAdapter(type);
	}

	public boolean doFocus() {
		var action = getActionRegistry().getAction(ActionIds.FOCUS);
		if (action.isEnabled()) {
			action.run();
			return true;
		}
		else return false;
	}

}
