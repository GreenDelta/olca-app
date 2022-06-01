package org.openlca.app.editors.graph;

import java.util.ArrayList;
import java.util.EventObject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.*;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.editors.graph.actions.AddExchangeAction;
import org.openlca.app.editors.graph.actions.AddProcessAction;
import org.openlca.app.editors.graph.actions.LayoutAction;
import org.openlca.app.editors.graph.edit.GraphEditPartFactory;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.GraphFactory;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ProductSystem;

/**
 * A {@link GraphEditor} is the starting point of the graphical interface of a
 * product system. It creates an <code>Editor</code> containing a single
 * <code>GraphicalViewer</code> as its control.
 * The <code>GraphModel</code>  is the head of the model to be further
 * displayed.
 */
public class GraphEditor extends GraphicalEditor {

	public static final String ID = "editors.graphical";

	private KeyHandler sharedKeyHandler;

	private final ProductSystemEditor systemEditor;
	private Graph graph;

	public static final double[] ZOOM_LEVELS = new double[] {
		0.01, 0.1, 0.2, 0.4, 0.8, 1.0, 1.6, 2.0, 3.0, 5.0, 10.0 };

	// TODO: save this in the same way as the layout is currently stored
	public final GraphConfig config = new GraphConfig();
	private final GraphFactory graphFactory = new GraphFactory(this);

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput graphInput) {
			if (graphInput.descriptor() != null) {
				setPartName(Labels.name(graphInput.descriptor()));
			}
		}
		super.init(site, input);
	}

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();

		GraphDropListener.on(this);

		// TODO (francois) Implement a PanningSelectionTool without pressing SpaceBar and
		//  Selection while pressing Ctrl.
		viewer.getEditDomain().setActiveTool(new PanningSelectionTool());

		viewer.setContents(getModel());
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();

		ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();

		// set clipping strategy for connection layer
		ConnectionLayer connectionLayer = (ConnectionLayer) root
			.getLayer(LayerConstants.CONNECTION_LAYER);
		connectionLayer
			.setClippingStrategy(new ViewportAwareConnectionLayerClippingStrategy(
				connectionLayer));

		var zoom = root.getZoomManager();
		zoom.setZoomLevels(ZOOM_LEVELS);
		var zoomLevels = new ArrayList<String>(3);
		zoomLevels.add(ZoomManager.FIT_ALL);
		zoomLevels.add(ZoomManager.FIT_WIDTH);
		zoomLevels.add(ZoomManager.FIT_HEIGHT);
		root.getZoomManager().setZoomLevelContributions(zoomLevels);
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
			MouseWheelZoomHandler.SINGLETON);
		var zoomIn = new ZoomInAction(root.getZoomManager());
		var zoomOut = new ZoomOutAction(root.getZoomManager());
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);
//		getSite().getKeyBindingService().registerAction(zoomIn);
//		getSite().getKeyBindingService().registerAction(zoomOut);

		viewer.setRootEditPart(root);
		viewer.setKeyHandler(getCommonKeyHandler());

		ContextMenuProvider provider = new GraphContextMenuProvider(viewer,
			getActionRegistry());
		viewer.setContextMenu(provider);

		viewer.setEditPartFactory(new GraphEditPartFactory());

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

		action = new LayoutAction(this);
		registry.registerAction(action);

		action = new AddProcessAction(this);
		registry.registerAction(action);

		action = new AddExchangeAction(this, true);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new AddExchangeAction(this, false);
		registry.registerAction(action);
		selectionActions.add(action.getId());
	}

	/**
	 * Returns the KeyHandler with common bindings for both the Outline (if it
	 * exists) and Graphical Views. For example, delete is a common action.
	 */
	protected KeyHandler getCommonKeyHandler() {
		if (sharedKeyHandler == null) {
			sharedKeyHandler = new KeyHandler();
			var registry = getActionRegistry();
			sharedKeyHandler.put(
				KeyStroke.getPressed(SWT.DEL, 127, 0),
				registry.getAction(ActionFactory.DELETE.getId()));
			sharedKeyHandler.put(
				KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				registry.getAction(GEFActionConstants.ZOOM_IN));
			sharedKeyHandler.put(
				KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				registry.getAction(GEFActionConstants.ZOOM_OUT));
		}
		return sharedKeyHandler;
	}

	protected void loadProperties() {
		// Zoom
		ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(
			ZoomManager.class.toString());
		if (manager != null)
			manager.setZoom(getModel().getZoom());
		// Scroll-wheel Zoom
		getGraphicalViewer().setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1),
			MouseWheelZoomHandler.SINGLETON);
	}

	protected void loadConfig() {
		// read GraphConfig object from file
		var config = GraphFile.getGraphConfig(this);
		if (config != null)
			config.copyTo(this.config);
	}

	/**
	 * The <code>selectionChanged</code> method of <code>GraphicalEditor</code> is
	 * overridden due to the update made on <code>getActiveEditor()</code> that
	 * now return a multi-page editor.
	 * @param part      the workbench part containing the selection
	 * @param selection the current selection. This may be <code>null</code> if
	 *                  <code>INullSelectionListener</code> is implemented.
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)	{
		if (getSite().getWorkbenchWindow().getActivePage().getActiveEditor().equals(this.systemEditor))
			updateActions(getSelectionActions());
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		var array = GraphFile.getLayouts(this);
		graph = getGraphFactory().createGraph(this, array);
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	public void setDirty() {
		systemEditor.setDirty(true);
	}

	@Override
	public boolean isDirty() {
		return systemEditor.isDirty();
	}

	public ProductSystem getProductSystem() {
		return systemEditor.getModel();
	}

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	public Graph getModel() {
		return graph;
	}

	public GraphFactory getGraphFactory() {
		return graphFactory;
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableFreeformRootEditPart getRootEditPart() {
		return (ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return getZoomManager();

		return super.getAdapter(type);
	}

}
