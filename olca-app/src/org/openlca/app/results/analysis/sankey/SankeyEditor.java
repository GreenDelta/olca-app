package org.openlca.app.results.analysis.sankey;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ViewportAwareConnectionLayerClippingStrategy;
import org.eclipse.gef.*;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchSizeAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.openlca.app.results.analysis.sankey.actions.*;
import org.openlca.app.results.analysis.sankey.model.SankeyFactory;
import org.openlca.app.tools.graphics.KeyHandler;
import org.openlca.app.tools.graphics.actions.ZoomInAction;
import org.openlca.app.tools.graphics.actions.ZoomOutAction;
import org.openlca.app.tools.graphics.edit.RootEditPart;
import org.openlca.app.tools.graphics.zoom.MouseWheelZoomHandler;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.analysis.sankey.edit.SankeyEditPartFactory;
import org.openlca.app.results.analysis.sankey.model.Diagram;
import org.openlca.app.tools.graphics.zoom.ZoomManager;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.model.RootEntity;
import org.openlca.core.results.LcaResult;
import org.openlca.core.results.ResultItemOrder;
import org.openlca.core.results.Sankey;

import java.util.ArrayList;
import java.util.EventObject;

public class SankeyEditor extends GraphicalEditor {

	public static final String ID = "editor.ProductSystemSankeyDiagram";
	public final ResultEditor resultEditor;
	public final DQResult dqResult;
	public final LcaResult result;
	public final ResultItemOrder items;
	public final RootEntity calculationTarget;

	// Set zoom levels from 0.1 to 3.0 with an incrementing factor of 5%.
	private static final int ZOOM_LEVELS_NUMBER =
			(int) Math.ceil(Math.log(3.0/0.1) / Math.log(1.05));
	public static final double[] ZOOM_LEVELS = new double[ZOOM_LEVELS_NUMBER];
	static {
		for (int i = 0; i < ZOOM_LEVELS_NUMBER; i++) {
			ZOOM_LEVELS[i] = Math.pow(1.05, i) * 0.1;
		}
	}

	private org.eclipse.gef.KeyHandler sharedKeyHandler;

	private final SankeyFactory sankeyFactory = new SankeyFactory(this);
	public final SankeyConfig config;
	private Diagram diagram;
	private Sankey<?> sankey;
	public boolean wasFocus = false;

	public SankeyEditor(ResultEditor parent) {
		this.resultEditor = parent;
		this.dqResult = parent.dqResult;
		this.result = parent.result;
		this.items = parent.items;
		this.calculationTarget = parent.setup.target();
		this.config = new SankeyConfig(this);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		setEditDomain(new DefaultEditDomain(this));
		if (calculationTarget != null) {
			setPartName(calculationTarget.name);
		}
		super.init(site, input);
	}

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
		var zoomLevels = new ArrayList<String>(3);
		zoomLevels.add(ZoomManager.FIT_ALL);
		zoomLevels.add(ZoomManager.FIT_WIDTH);
		zoomLevels.add(ZoomManager.FIT_HEIGHT);
		root.getZoomManager().setZoomLevelContributions(zoomLevels);
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		var zoomIn = new ZoomInAction(root.getZoomManager());
		var zoomOut = new ZoomOutAction(root.getZoomManager());
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);
//		getSite().getKeyBindingService().registerAction(zoomIn);
//		getSite().getKeyBindingService().registerAction(zoomOut);

		viewer.setRootEditPart(root);
		viewer.setKeyHandler(createGraphKeyHandler());

		ContextMenuProvider provider = new SankeyContextMenuProvider(viewer,
				getActionRegistry());
		viewer.setContextMenu(provider);

		viewer.setEditPartFactory(new SankeyEditPartFactory());

		loadProperties();
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		// The model is initialized with an empty diagram so that it is only created
		// when the SankeyDiagram page is open (see ResultEditor).
		setModel(new Diagram(this, config.orientation()));
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

		action = new OpenEditorAction(this);
		registry.registerAction(action);
		selectionActions.add(action.getId());

		action = new SaveImageAction(this);
		registry.registerAction(action);

		action = new EditSankeyConfigAction(this);
		registry.registerAction(action);

		action = new OpenMiniatureViewAction(this);
		registry.registerAction(action);

		action = new FocusAction(this);
		registry.registerAction(action);

		action = new LayoutAction(this);
		registry.registerAction(action);
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	public RootEditPart getRootEditPart() {
		return (RootEditPart) getGraphicalViewer().getRootEditPart();
	}


	public SankeyFactory getSankeyFactory() {
		return sankeyFactory;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	public Object getAdapter(Class type) {
		if (type == org.openlca.app.tools.graphics.zoom.ZoomManager.class)
			return getZoomManager();

		return super.getAdapter(type);
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
		if (activePage.getActiveEditor().equals(this.resultEditor))
			updateActions(getSelectionActions());
	}

	public Diagram getModel() {
		return diagram;
	}

	public void setModel(Diagram model) {
		this.diagram = model;
	}

	public void setSankey(Sankey<?> sankey) {
		this.sankey = sankey;
	}

	public Sankey<?> getSankey() {
		return sankey;
	}

	public boolean focusOnReferenceNode() {
		var action = getActionRegistry().getAction(ActionIds.FOCUS);
		if (action.isEnabled()) {
			action.run();
			return true;
		}
		else return false;
	}

}
