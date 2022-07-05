package org.openlca.app.editors.graphical;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ViewportAwareConnectionLayerClippingStrategy;
import org.eclipse.gef.*;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.actions.*;
import org.openlca.app.editors.graphical.actions.ZoomInAction;
import org.openlca.app.editors.graphical.actions.ZoomOutAction;
import org.openlca.app.editors.graphical.edit.GraphEditPartFactory;
import org.openlca.app.editors.graphical.edit.GraphRoot;
import org.openlca.app.editors.graphical.model.Graph;
import org.openlca.app.editors.graphical.model.GraphFactory;
import org.openlca.app.editors.graphical.zoom.GraphMouseWheelZoomHandler;
import org.openlca.app.editors.graphical.zoom.GraphZoomManager;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.ProductSystem;

import java.util.ArrayList;
import java.util.EventObject;

import static org.openlca.app.editors.graphical.actions.MassExpansionAction.COLLAPSE;
import static org.openlca.app.editors.graphical.actions.MassExpansionAction.EXPAND;
import static org.openlca.app.editors.graphical.actions.SearchConnectorsAction.PROVIDER;
import static org.openlca.app.editors.graphical.actions.SearchConnectorsAction.RECIPIENTS;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MAXIMIZE;
import static org.openlca.app.editors.graphical.model.commands.MinMaxCommand.MINIMIZE;

/**
 * A {@link GraphEditor} is the starting point of the graphical interface of a
 * product system. It creates an <code>Editor</code> containing a single
 * <code>GraphicalViewer</code> as its control.
 * The <code>GraphModel</code>  is the head of the model to be further
 * displayed.
 */
public class GraphEditor extends GraphicalEditor {

	public static final String ID = "GraphicalEditor";

	private KeyHandler sharedKeyHandler;
	private final ProductSystemEditor systemEditor;
	private Graph graph;

	// Set zoom levels from 0.1 to 3.0 with an incrementing factor of 5%.
	private static final int ZOOM_LEVELS_NUMBER =
		(int) Math.ceil(Math.log(3.0/0.1) / Math.log(1.05));
	public static final double[] ZOOM_LEVELS = new double[ZOOM_LEVELS_NUMBER];
	static {
		for (int i = 0; i < ZOOM_LEVELS_NUMBER; i++) {
			ZOOM_LEVELS[i] = Math.pow(1.05, i) * 0.1;
		}
	}

	// TODO: save this in the same way as the layout is currently stored
	public final GraphConfig config = new GraphConfig();
	private final GraphFactory graphFactory = new GraphFactory(this);

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
		editor.onSaved(() -> GraphFile.save(this));
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

		var root = new GraphRoot(viewer);

		// set clipping strategy for connection layer
		ConnectionLayer connectionLayer = (ConnectionLayer) root
			.getLayer(LayerConstants.CONNECTION_LAYER);
		connectionLayer
			.setClippingStrategy(new ViewportAwareConnectionLayerClippingStrategy(
				connectionLayer));

		var zoom = root.getZoomManager();
		zoom.setZoomLevels(ZOOM_LEVELS);
		var zoomLevels = new ArrayList<String>(3);
		zoomLevels.add(GraphZoomManager.FIT_ALL);
		zoomLevels.add(GraphZoomManager.FIT_WIDTH);
		zoomLevels.add(GraphZoomManager.FIT_HEIGHT);
		root.getZoomManager().setZoomLevelContributions(zoomLevels);
		zoom.setZoomAnimationStyle(GraphZoomManager.ANIMATE_ZOOM_IN_OUT);
		var zoomIn = new ZoomInAction(root.getZoomManager());
		var zoomOut = new ZoomOutAction(root.getZoomManager());
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);
//		getSite().getKeyBindingService().registerAction(zoomIn);
//		getSite().getKeyBindingService().registerAction(zoomOut);

		viewer.setRootEditPart(root);
		viewer.setKeyHandler(createGraphKeyHandler());

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
		var stackActions = getStackActions();
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

		action = new MinMaxAllAction(this, MINIMIZE);
		registry.registerAction(action);
		stackActions.add(action.getId());

		action = new MinMaxAllAction(this, MAXIMIZE);
		registry.registerAction(action);
		stackActions.add(action.getId());

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

		action = new EditExchangeAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new EditGraphConfigAction(this);
		registry.registerAction(action);

		action = new OpenEditorAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new OpenMiniatureViewAction(this);
		registry.registerAction(action);

		action = new SaveImageAction(this);
		registry.registerAction(action);

		action = new MassExpansionAction(this, EXPAND);
		registry.registerAction(action);
		stackActions.add(action.getId());

		action = new MassExpansionAction(this, COLLAPSE);
		registry.registerAction(action);
		stackActions.add(action.getId());

		// TODO (francois) Too slow.
//		action = new RemoveAllConnectionsAction(this);
//		registry.registerAction(action);
//		selectionActions.add(action.getId());

		action = new BuildSupplyChainMenuAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new BuildNextTierAction();
		registry.registerAction(action);

		action = new BuildSupplyChainAction();
		registry.registerAction(action);

		action = new RemoveSupplyChainAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new LinkUpdateAction(this);
		registry.registerAction(action);

		action = new SearchConnectorsAction(this, PROVIDER);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new SearchConnectorsAction(this, RECIPIENTS);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new FocusAction(this);
		registry.registerAction(action);
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
		}
		return sharedKeyHandler;
	}

	protected KeyHandler createGraphKeyHandler() {
		var keyHandler = new GraphKeyHandler(getGraphicalViewer());
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

	protected void loadProperties() {
		// Zoom
		var manager = getZoomManager();
		if (manager != null)
			manager.setZoom(getModel().getZoom());
		getGraphicalViewer().setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
			GraphMouseWheelZoomHandler.SINGLETON);
	}

	protected void loadConfig() {
		// read GraphConfig object from file
		var config = GraphFile.getGraphConfig(this);
		if (config != null)
			config.copyTo(this.config);
	}

	/**
	 * The <code>selectionChanged</code> method of <code>GraphicalEditor</code> is
	 * overridden due to the fact that this <code>GraphicalEditor</code> us part
	 * of a multipage editor.
	 * @param part      the workbench part containing the selection
	 * @param selection the current selection. This may be <code>null</code> if
	 *                  <code>INullSelectionListener</code> is implemented.
	 */
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)	{
		var activePage = getSite().getWorkbenchWindow().getActivePage();
		if (activePage.getActiveEditor().equals(this.systemEditor))
			updateActions(getSelectionActions());
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		var array = GraphFile.getLayouts(this);
		setModel(getGraphFactory().createGraph(this, array));
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

	public void setModel(Graph model) {
		graph = model;
	}

	public Graph getModel() {
		return graph;
	}

	public GraphFactory getGraphFactory() {
		return graphFactory;
	}

	public GraphZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	public GraphRoot getRootEditPart() {
		return (GraphRoot) getGraphicalViewer().getRootEditPart();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		systemEditor.doSave(monitor);
	}

	public boolean promptSaveIfNecessary() throws Exception {
		if (!isDirty())
			return true;
		String question = M.SystemSaveProceedQuestion;
		if (Question.ask(M.Save + "?", question)) {
			new ProgressMonitorDialog(UI.shell()).run(
				false, false, systemEditor::doSave);
			return true;
		}
		return false;
	}

	public void updateModel(IProgressMonitor monitor) {
		monitor.beginTask(M.UpdatingProductSystem, IProgressMonitor.UNKNOWN);
		systemEditor.updateModel();
		monitor.done();
	}

	public Object getAdapter(Class type) {
		if (type == GraphZoomManager.class)
			return getZoomManager();

		return super.getAdapter(type);
	}

	public ProductSystemEditor getProductSystemEditor() {
		return systemEditor;
	}

	public void focusOnReferenceNode() {
		var action = getActionRegistry().getAction(ActionIds.FOCUS);
		if (action.isEnabled()) action.run();
	}

}
