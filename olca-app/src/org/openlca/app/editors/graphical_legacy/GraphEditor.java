package org.openlca.app.editors.graphical_legacy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.action.BuildSupplyChainMenuAction;
import org.openlca.app.editors.graphical_legacy.action.GraphActions;
import org.openlca.app.editors.graphical_legacy.action.LayoutMenuAction;
import org.openlca.app.editors.graphical_legacy.command.LayoutCommand;
import org.openlca.app.editors.graphical_legacy.layout.LayoutManager;
import org.openlca.app.editors.graphical_legacy.layout.LayoutType;
import org.openlca.app.editors.graphical_legacy.model.GraphEditPartFactory;
import org.openlca.app.editors.graphical_legacy.model.Link;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;
import org.openlca.app.editors.graphical_legacy.model.ProductSystemNode;
import org.openlca.app.editors.graphical_legacy.outline.OutlinePage;
import org.openlca.app.editors.graphical_legacy.search.MutableProcessLinkSearchMap;
import org.openlca.app.editors.graphical_legacy.view.TreeConnectionRouter;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;

/**
 * A model graph consists in a {@link GraphicalEditor} that will display the
 * model. That {@link GraphicalEditor} displays a
 * {@link org.openlca.app.editors.graphical_legacy.model.ProductSystemNode} and a set
 * of {@link org.openlca.app.editors.graphical_legacy.model.Link}s.
 */
public class GraphEditor extends GraphicalEditor {

	public static final String ID = "editors.productsystem.graphical";
	public static final double[] ZOOM_LEVELS = new double[] {
			0.01, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0 };

	// TODO: save this in the same way like the layout is currently stored
	public final GraphConfig config = new GraphConfig();

	private final ProductSystemEditor systemEditor;
	private final LayoutType layoutType = LayoutType.TREE_LAYOUT;

	private ProductSystemNode model;
	private OutlinePage outline;
	private ISelection selection;

	// TODO: we may do not need this later when we build our
	// context menu more selection specific.
	private final List<String> updateActions = new ArrayList<>();

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
		editor.onSaved(() -> GraphFile.save(this));
	}

	public ProductSystemEditor systemEditor() {
		return systemEditor;
	}

	public ProductSystem getProductSystem() {
		return systemEditor.getModel();
	}

	public void setDirty() {
		systemEditor.setDirty(true);
	}

	@Override
	public boolean isDirty() {
		return systemEditor.isDirty();
	}

	public ISelection getSelection() {
		return selection;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput) {
			var ginp = (GraphicalEditorInput) input;
			if (ginp.getDescriptor() != null) {
				setPartName(Labels.name(ginp.getDescriptor()));
			}
		}
		super.init(site, input);
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

	public void createNecessaryLinks(ProcessNode node) {
		MutableProcessLinkSearchMap linkSearch = node.parent().linkSearch;
		ProductSystemNode sysNode = node.parent();
		long id = node.process.id;
		for (ProcessLink pLink : linkSearch.getLinks(id)) {
			boolean isProvider = pLink.providerId == id;
			long otherID = isProvider ? pLink.processId : pLink.providerId;
			ProcessNode otherNode = model.getProcessNode(otherID);
			if (otherNode == null)
				continue;
			ProcessNode outNode = null;
			ProcessNode inNode = null;
			FlowType type = sysNode.flows.type(pLink.flowId);
			if (type == FlowType.PRODUCT_FLOW) {
				outNode = isProvider ? node : otherNode;
				inNode = isProvider ? otherNode : node;
			} else if (type == FlowType.WASTE_FLOW) {
				outNode = isProvider ? otherNode : node;
				inNode = isProvider ? node : otherNode;
			}
			if (outNode == null)
				continue;
			if (!outNode.isExpandedRight() && !inNode.isExpandedLeft())
				continue;
			Link link = new Link();
			link.outputNode = outNode;
			link.inputNode = inNode;
			link.processLink = pLink;
			link.link();
		}
	}

	@Override
	protected void configureGraphicalViewer() {
		model = new ProductSystemNode(this);
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new GraphEditPartFactory());
		viewer.setRootEditPart(new ScalableRootEditPart());
		var actions = configureActions();
		var keyHandler = new KeyHandler();
		IAction delete = actions.getAction(org.eclipse.ui.actions.ActionFactory.DELETE.getId());
		IAction zoomIn = actions.getAction(GEFActionConstants.ZOOM_IN);
		IAction zoomOut = actions.getAction(GEFActionConstants.ZOOM_OUT);
		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), delete);
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), zoomIn);
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), zoomOut);
		viewer.setKeyHandler(keyHandler);
		new MenuProvider(this, getActionRegistry());
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
				new BuildSupplyChainMenuAction(this),
				GraphActions.removeSupplyChain(this),
				GraphActions.removeAllConnections(this),
				GraphActions.expandAll(this),
				GraphActions.collapseAll(this),
				GraphActions.maximizeAll(this),
				GraphActions.minimizeAll(this),
				new LayoutMenuAction(this),
				GraphActions.searchProviders(this),
				GraphActions.searchRecipients(this),
				GraphActions.openMiniatureView(this),
				GraphActions.showOutline(),
				new ZoomInAction(getZoomManager()),
				new ZoomOutAction(getZoomManager()),
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

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();
		viewer.setRootEditPart(new ScalableRootEditPart() {

			@Override
			protected LayeredPane createPrintableLayers() {
				var pane = new LayeredPane();

				var models = new Layer();
				models.setOpaque(false);
				models.setLayoutManager(new StackLayout());
				pane.add(models, PRIMARY_LAYER);

				// add the connection layer on top of the
				// model layer
				var connections = new ConnectionLayer();
				connections.setPreferredSize(new Dimension(5, 5));
				pane.add(connections, CONNECTION_LAYER);

				return pane;
			}
		});

		GraphDropListener.on(this);
		viewer.getEditDomain().setActiveTool(
				new PanningSelectionTool());
		viewer.setContents(model);
		var zoom = getZoomManager();
		zoom.setZoomLevels(ZOOM_LEVELS);
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(
				MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);

		// load the graph settings
		var fileApplied = GraphFile.apply(this);
		if (!fileApplied) {
			// no saved settings applied =>
			// try to find a good configuration
			var system = systemEditor.getModel();
			if (system.referenceProcess != null) {
				var refNode = model.getProcessNode(
						system.referenceProcess.id);
				if (refNode != null) {
					refNode.expandLeft();
					refNode.expandRight();
				}
			}

			// initialize the tree layout
			if (model != null && model.figure != null) {
				var layout = model.figure.getLayoutManager();
				if (layout instanceof LayoutManager) {
					((LayoutManager) layout).layout(
							model.figure, LayoutType.TREE_LAYOUT);
				}
			}
		}
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		systemEditor.doSave(monitor);
	}

	public void updateModel(IProgressMonitor monitor) {
		monitor.beginTask(M.UpdatingProductSystem, IProgressMonitor.UNKNOWN);
		systemEditor.updateModel();
		monitor.done();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return ((ScalableRootEditPart) getGraphicalViewer()
				.getRootEditPart())
				.getZoomManager();
		if (type == IContentOutlinePage.class) {
			outline = new OutlinePage(model);
			outline.setEditDomain(getEditDomain());
			outline.setSelectionSynchronizer(getSelectionSynchronizer());
			return outline;
		}
		return super.getAdapter(type);
	}

	@Override
	public CommandStack getCommandStack() {
		var stack = super.getCommandStack();
		if (stack == null) {
			stack = new CommandStack();
		}
		return stack;
	}

	public LayoutType getLayoutType() {
		return layoutType;
	}

	public ProductSystemNode getModel() {
		return model;
	}

	public OutlinePage getOutline() {
		return outline;
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) getGraphicalViewer().getRootEditPart();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Expands all process nodes in the graphical editor.
	 */
	public void expand() {
		model = new ProductSystemNode(this);
		var system = getProductSystem();

		// create the process nodes
		var ref = system.referenceProcess;
		for (var id : system.processes) {
			if (id == null)
				continue;
			// skip the reference process as this
			// was already added
			if (ref != null && ref.id == id)
				continue;
			var node = ProcessNode.create(this, id);
			if (node != null) {
				model.add(node);
			}
		}

		// set the viewer content and expand the nodes
		var viewer = getGraphicalViewer();
		if (viewer == null)
			return;
		viewer.deselectAll();
		viewer.setContents(model);
		for (var node : model.getChildren()) {
			node.expandLeft();
			node.expandRight();
		}
		getCommandStack().execute(
			new LayoutCommand(this, LayoutType.TREE_LAYOUT));
	}

	/**
	 * Collapse all process nodes in the editor. Only the reference process will be
	 * added.
	 */
	public void collapse() {
		model = new ProductSystemNode(this);
		var viewer = getGraphicalViewer();
		if (viewer == null)
			return;
		viewer.deselectAll();
		viewer.setContents(model);
		getCommandStack().execute(
			new LayoutCommand(this, LayoutType.TREE_LAYOUT));
	}

	/**
	 * Calls refresh on all created edit parts.
	 */
	public void refresh() {
		var viewer = getGraphicalViewer();
		if (viewer == null)
			return;
		var queue = new ArrayDeque<EditPart>();
		queue.add(viewer.getRootEditPart());
		while (!queue.isEmpty()) {
			var part = queue.poll();
			part.refresh();
			for (var child : part.getChildren()) {
				if (child instanceof EditPart) {
					queue.add((EditPart) child);
				}
			}
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		this.selection = selection;
		updateActions(updateActions);
	}

	@Override
	public void setFocus() {
		if (getGraphicalViewer() == null)
			return;
		if (getGraphicalViewer().getControl() == null)
			return;
		super.setFocus();
	}

	public void route() {
		var router = config.isRouted
				? TreeConnectionRouter.instance
				: ConnectionRouter.NULL;
		for (var node : model.getChildren()) {
			for (var link : node.links) {
				link.figure.setConnectionRouter(router);
			}
		}
	}
}
