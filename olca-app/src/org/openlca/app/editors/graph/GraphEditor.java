package org.openlca.app.editors.graph;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.*;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.*;
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

	private final ProductSystemEditor systemEditor;
	private Graph graph;
	private ISelection selection;

	// TODO: we may do not need this later when we build our
	//  context menu more selection specific.
	private final List<String> updateActions = new ArrayList<>();

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
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
			MouseWheelZoomHandler.SINGLETON);

		viewer.setRootEditPart(root);

		var actions = configureActions();
		var keyHandler = new KeyHandler();
		IAction delete = actions.getAction(org.eclipse.ui.actions.ActionFactory.DELETE.getId());
		IAction zoomIn = actions.getAction(GEFActionConstants.ZOOM_IN);
		IAction zoomOut = actions.getAction(GEFActionConstants.ZOOM_OUT);
		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), delete);
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), zoomIn);
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), zoomOut);
		viewer.setKeyHandler(keyHandler);

		ContextMenuProvider provider = new GraphContextMenuProvider(viewer,
			getActionRegistry());
		viewer.setContextMenu(provider);

		viewer.setEditPartFactory(new GraphEditPartFactory());

		loadProperties();
		loadConfig();
	}


	private ActionRegistry configureActions() {
		var delete = new DeleteAction((IWorkbenchPart) this) {
			@Override
			protected ISelection getSelection() {
				return getSite()
					.getWorkbenchWindow()
					.getSelectionService()
					.getSelection();
			}
		};

		var actions = new IAction[] {
			new ZoomInAction(getZoomManager()),
			new ZoomOutAction(getZoomManager()),
			new LayoutAction(this),
			delete,
		};

		var registry = getActionRegistry();
		for (var action : actions) {
			registry.registerAction(action);
			if (action instanceof UpdateAction) {
				updateActions.add(action.getId());
			}
		}
		return registry;
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

	public ISelection getSelection() {
		return selection;
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

	public void setGraphModel(Graph model) {
		graph = model;
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

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		this.selection = selection;
		updateActions(updateActions);
	}

	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return ((ScalableFreeformRootEditPart) getGraphicalViewer()
				.getRootEditPart())
				.getZoomManager();

		return super.getAdapter(type);
	}
}
