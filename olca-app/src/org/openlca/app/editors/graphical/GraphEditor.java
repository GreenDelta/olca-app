package org.openlca.app.editors.graphical;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.app.M;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.action.AddProcessAction;
import org.openlca.app.editors.graphical.action.BuildSupplyChainMenuAction;
import org.openlca.app.editors.graphical.action.GraphActions;
import org.openlca.app.editors.graphical.action.LayoutMenuAction;
import org.openlca.app.editors.graphical.layout.LayoutType;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;
import org.openlca.app.editors.graphical.model.AppEditPartFactory;
import org.openlca.app.editors.graphical.model.Link;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.model.TreeConnectionRouter;
import org.openlca.app.editors.graphical.outline.OutlinePage;
import org.openlca.app.editors.graphical.search.MutableProcessLinkSearchMap;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Question;
import org.openlca.app.util.UI;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ProcessLink;

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
	private boolean routed;
	private ISelection selection;
	private boolean initialized = false;

	// TODO: we may do not need this later when we build our
	// context menu more selection specific.
	private final List<String> updateActions = new ArrayList<>();

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
		editor.onSaved(() -> NodeLayoutStore.saveLayout(getModel()));
		// draw nice routes when there are less then 100 processes
		// in the system
		routed = editor.getModel().processes.size() <= 100;
	}

	public ProductSystemEditor getSystemEditor() {
		return systemEditor;
	}

	public void setDirty(boolean value) {
		systemEditor.setDirty(value);
	}

	@Override
	public boolean isDirty() {
		return systemEditor.isDirty();
	}

	public ISelection getSelection() {
		return selection;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public boolean isInitialized() {
		return initialized;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput) {
			var modelInput = (GraphicalEditorInput) input;
			if (modelInput.getDescriptor() != null) {
				setPartName(Labels.name(modelInput.getDescriptor()));
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

	private ProductSystemNode createModel() {
		var node = new ProductSystemNode(this);
		var refProcess = getSystemEditor().getModel().referenceProcess;
		if (refProcess == null)
			return node;
		ProcessNode p = ProcessNode.create(this, refProcess.id);
		if (p != null) {
			node.add(p);
		}
		return node;
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
		model = createModel();
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new AppEditPartFactory());
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
				new AddProcessAction(this),
				new BuildSupplyChainMenuAction(this),
				GraphActions.removeSupplyChain(this),
				GraphActions.removeAllConnections(this),
				GraphActions.saveImage(this),
				GraphActions.expandAll(this),
				GraphActions.collapseAll(this),
				GraphActions.maximizeAll(this),
				GraphActions.minimizeAll(this),
				new LayoutMenuAction(this),
				GraphActions.searchProviders(this),
				GraphActions.searchRecipients(this),
				GraphActions.open(this),
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
				LayeredPane pane = new LayeredPane();
				Layer layer = new ConnectionLayer();
				layer.setPreferredSize(new Dimension(5, 5));
				pane.add(layer, CONNECTION_LAYER);
				layer = new Layer();
				layer.setOpaque(false);
				layer.setLayoutManager(new StackLayout());
				pane.add(layer, PRIMARY_LAYER);
				return pane;
			}
		});

		var transfer = ModelTransfer.getInstance();
		var dropTarget = new DropTarget(viewer.getControl(),
				DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(transfer);
		dropTarget.addDropListener(new GraphDropListener(
				model, transfer, getCommandStack()));
		viewer.getEditDomain().setActiveTool(
				new PanningSelectionTool());
		viewer.setContents(model);

		getZoomManager().setZoomLevels(ZOOM_LEVELS);
		getZoomManager().setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(
				MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);
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
			return ((ScalableRootEditPart) getGraphicalViewer().getRootEditPart()).getZoomManager();
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
		getSystemEditor().getModel().processes.stream()
				.filter(Objects::nonNull)
				.map(id -> ProcessNode.create(this, id))
				.filter(Objects::nonNull)
				.forEach(node -> model.add(node));
		var viewer = getGraphicalViewer();
		if (viewer == null)
			return;
		viewer.deselectAll();
		viewer.setContents(model);
		for (var node : model.getChildren()) {
			node.expandLeft();
			node.expandRight();
		}
	}

	/**
	 * Collapse all process nodes in the editor. Only the reference process will be
	 * added.
	 */
	public void collapse() {
		model = createModel();
		var viewer = getGraphicalViewer();
		if (viewer == null)
			return;
		viewer.deselectAll();
		viewer.setContents(model);
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

	public boolean isRouted() {
		return routed;
	}

	public void setRouted(boolean routed) {
		this.routed = routed;
		var router = routed
				? TreeConnectionRouter.instance
				: ConnectionRouter.NULL;
		for (var node : model.getChildren()) {
			for (var link : node.links) {
				link.figure.setConnectionRouter(router);
			}
		}
	}
}
