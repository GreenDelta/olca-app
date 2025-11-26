package org.openlca.app.editors.sd.editor.graph;

import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ViewportAwareConnectionLayerClippingStrategy;
import org.eclipse.draw2d.geometry.Point;
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
import org.openlca.app.editors.sd.editor.graph.edit.SdNodeEditPart;
import org.openlca.app.editors.sd.editor.graph.model.SdGraph;
import org.openlca.app.editors.sd.editor.graph.model.SdLink;
import org.openlca.app.editors.sd.editor.graph.model.SdNode;
import org.openlca.app.editors.sd.editor.graph.model.SdNodeType;
import org.openlca.sd.eqn.Id;
import org.openlca.sd.eqn.Var;
import org.openlca.sd.eqn.Var.Stock;

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
		graph = new SdGraph(this);

		// Create nodes from model variables
		var vars = modelEditor.vars();
		int stockX = 50, stockY = 50;
		int rateX = 200, rateY = 50;
		int auxX = 350, auxY = 50;

		for (var v : vars) {
			var nodeType = nodeTypeOf(v);
			if (nodeType == null)
				continue;

			var name = v.name() != null ? v.name().label() : "unknown";
			var node = new SdNode(nodeType, name);
			// TODO: node.bindVariable(v);

			// Position nodes in columns by type
			switch (nodeType) {
				case STOCK -> {
					node.setLocation(new Point(stockX, stockY));
					stockY += 100;
				}
				case RATE -> {
					node.setLocation(new Point(rateX, rateY));
					rateY += 80;
				}
				case AUXILIARY -> {
					node.setLocation(new Point(auxX, auxY));
					auxY += 60;
				}
			}

			graph.addNode(node);
		}

		var nodeIndex = new HashMap<Id, SdNode>();
		for (var node : graph.getNodes()) {
			for (var v : vars) {
				if (v.name() != null && v.name().label().equals(node.getVariableName())) {
					nodeIndex.put(v.name(), node);
					break;
				}
			}
		}

		// Create flow links between rates and stocks
		// Links must be created BEFORE setContents so EditParts get them
		for (var v : vars) {
			if (v instanceof Stock s) {
				var stockNode = nodeIndex.get(s.name());
				if (stockNode == null)
					continue;

				// Inflows: rate -> stock
				for (var id : s.inFlows()) {
					var rateNode = nodeIndex.get(id);
					if (rateNode != null) {
						var link = new SdLink(rateNode, stockNode, true);
						link.connect();
					}
				}

				// Outflows: stock -> rate
				for (var id : s.outFlows()) {
					var rateNode = nodeIndex.get(id);
					if (rateNode != null) {
						var link = new SdLink(stockNode, rateNode, true);
						link.connect();
					}
				}
			}
		}

		// Set contents after all nodes and links are created
		getGraphicalViewer().setContents(graph);

		// Use asyncExec to ensure layout is complete before refreshing connections
		var display = getGraphicalViewer().getControl().getDisplay();
		display.asyncExec(() -> {
			var contents = (RootEditPart) getGraphicalViewer().getContents();
			if (contents != null) {
				// Force layout validation so figures have proper bounds
				contents.getFigure().invalidate();
				contents.getFigure().validate();

				// Refresh all node EditParts to create/update connection EditParts
				for (var child : contents.getChildren()) {
					if (child instanceof SdNodeEditPart nodeEditPart) {
						nodeEditPart.refresh();
					}
				}
			}
		});
	}

	private SdNodeType nodeTypeOf(Var v) {
		return switch (v) {
			case Var.Stock ignored -> SdNodeType.STOCK;
			case Var.Rate ignored -> SdNodeType.RATE;
			case Var.Aux ignored -> SdNodeType.AUXILIARY;
			case null -> null;
		};
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
