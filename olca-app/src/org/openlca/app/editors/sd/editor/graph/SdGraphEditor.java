package org.openlca.app.editors.sd.editor.graph;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ViewportAwareConnectionLayerClippingStrategy;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.components.graphics.actions.ZoomInAction;
import org.openlca.app.components.graphics.actions.ZoomOutAction;
import org.openlca.app.components.graphics.edit.RootEditPart;
import org.openlca.app.components.graphics.tools.PanningSelectionTool;
import org.openlca.app.components.graphics.zoom.MouseWheelZoomHandler;
import org.openlca.app.components.graphics.zoom.ZoomManager;
import org.openlca.app.editors.sd.editor.SdModelEditor;
import org.openlca.app.editors.sd.editor.graph.actions.AddAuxiliaryAction;
import org.openlca.app.editors.sd.editor.graph.actions.AddRateAction;
import org.openlca.app.editors.sd.editor.graph.actions.AddStockAction;
import org.openlca.app.editors.sd.editor.graph.edit.SdEditPartFactory;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;

/**
 * Graphical editor for system dynamics models.
 * Allows editing stocks, rates, auxiliaries and their connections.
 */
public class SdGraphEditor extends GraphicalEditor {

	public static final String ID = "SdGraphEditor";

	private final SdModelEditor modelEditor;
	private SdGraph graph;
	private KeyHandler keyHandler;

	public SdGraphEditor(SdModelEditor modelEditor) {
		this.modelEditor = modelEditor;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		super.init(site, input);
		if (input instanceof SdGraphEditorInput graphInput) {
			setPartName(graphInput.getName());
		}
	}

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();
		viewer.getEditDomain().setActiveTool(new PanningSelectionTool());
		viewer.setContents(graph);
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();

		var root = new RootEditPart(viewer);

		// Set clipping strategy for connection layer
		var connectionLayer = (ConnectionLayer) root
				.getLayer(LayerConstants.CONNECTION_LAYER);
		connectionLayer.setClippingStrategy(
				new ViewportAwareConnectionLayerClippingStrategy(connectionLayer));

		// Register zoom actions
		var zoom = createZoom(root);
		var zoomIn = new ZoomInAction(zoom);
		var zoomOut = new ZoomOutAction(zoom);
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);

		viewer.setRootEditPart(root);
		viewer.setKeyHandler(createKeyHandler());
		viewer.setEditPartFactory(new SdEditPartFactory());

		// Configure mouse wheel zoom
		viewer.setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
			MouseWheelZoomHandler.SINGLETON);

		// Set up context menu
		var menuProvider = new ContextMenu(viewer, getActionRegistry());
		viewer.setContextMenu(menuProvider);
	}

	private ZoomManager createZoom(RootEditPart root) {
		int len = (int) Math.ceil(Math.log(3.0 / 0.1) / Math.log(1.05));
		var levels = new double[len];
		for (int i = 0; i < len; i++) {
			levels[i] = Math.pow(1.05, i) * 0.1;
		}
		var zoom = root.getZoomManager();
		zoom.setZoomLevels(levels);
		zoom.setZoomLevelContributions(List.of(
			ZoomManager.FIT_ALL,
			ZoomManager.FIT_HEIGHT,
			ZoomManager.FIT_WIDTH));
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		return zoom;
	}

	@Override
	protected void createActions() {
		super.createActions();
		var registry = getActionRegistry();

		IAction action;

		action = new AddStockAction(this);
		registry.registerAction(action);

		action = new AddRateAction(this);
		registry.registerAction(action);

		action = new AddAuxiliaryAction(this);
		registry.registerAction(action);
	}

	private KeyHandler createKeyHandler() {
		var keyHandler = new DefaultKeyHandler(getGraphicalViewer());
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

	private KeyHandler getCommonKeyHandler() {
		if (keyHandler == null) {
			keyHandler = new KeyHandler();
			var registry = getActionRegistry();
			keyHandler.put(
					KeyStroke.getPressed(SWT.DEL, 127, 0),
					registry.getAction(ActionFactory.DELETE.getId()));
		}
		return keyHandler;
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		graph = new SdGraph(this);
	}

	public SdGraph getModel() {
		return graph;
	}

	public SdModelEditor getModelEditor() {
		return modelEditor;
	}

	public RootEditPart getRootEditPart() {
		return (RootEditPart) getGraphicalViewer().getRootEditPart();
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	@Override
	public ActionRegistry getActionRegistry() {
		return super.getActionRegistry();
	}

	/**
	 * Called when the graph page is first activated.
	 * Loads the graph from the model.
	 */
	public void onFirstActivation() {
		// TODO: Initialize the graph from the actual SD model (XMILE)
		// For now, create an empty graph
		graph = new SdGraph(this);
		getGraphicalViewer().setContents(graph);

		// TODO: Load nodes from model variables
		// var vars = modelEditor.vars();
		// for (var v : vars) {
		//     var nodeType = determineNodeType(v);
		//     var node = new SdNode(nodeType, v.name().label());
		//     graph.addNode(node);
		// }
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		updateActions(getSelectionActions());
	}

	@Override
	public boolean isDirty() {
		return modelEditor.isDirty();
	}

	public void setDirty() {
		modelEditor.setDirty();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// Saving is handled by the parent SdModelEditor
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return getZoomManager();
		return super.getAdapter(type);
	}
}
