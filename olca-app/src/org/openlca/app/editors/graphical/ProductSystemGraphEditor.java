package org.openlca.app.editors.graphical;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical.layout.LayoutType;
import org.openlca.app.editors.graphical.layout.NodeLayoutStore;
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
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductSystemGraphEditor extends GraphicalEditor {

	public static final String ID = "editors.productsystem.graphical";

	private ProductSystemEditor systemEditor;
	private ProductSystemNode model;
	private LayoutType layoutType = LayoutType.TREE_LAYOUT;
	private OutlinePage outline;
	private boolean routed;
	private GraphConfig config;
	private ISelection selection;
	private List<String> actionIds;
	private boolean initialized = false;

	public ProductSystemGraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
		editor.onSaved(() -> NodeLayoutStore.saveLayout(getModel()));
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
			GraphicalEditorInput modelInput = (GraphicalEditorInput) input;
			if (modelInput.getDescriptor() != null) {
				setPartName(Labels.getDisplayName(modelInput.getDescriptor()));
			}
		}
		super.init(site, input);
	}

	public boolean promptSaveIfNecessary() throws Exception {
		if (!isDirty())
			return true;
		String question = M.SystemSaveProceedQuestion;
		if (Question.ask(M.Save + "?", question)) {
			new ProgressMonitorDialog(UI.shell()).run(false, false, (monitor) -> systemEditor.doSave(monitor));
			return true;
		}
		return false;
	}

	private ProductSystemNode createModel() {
		Process refProcess = getSystemEditor().getModel().referenceProcess;
		if (refProcess == null)
			return new ProductSystemNode(this);
		long referenceId = refProcess.id;
		ProductSystemNode node = new ProductSystemNode(this);
		node.add(createProcessNode(referenceId));
		return node;
	}

	private ProductSystemNode expandModel() {
		ProductSystemNode node = new ProductSystemNode(this);
		for (Long id : getSystemEditor().getModel().processes)
			node.add(createProcessNode(id));
		return node;
	}

	private ProcessNode createProcessNode(long id) {
		ProcessDescriptor descriptor = Cache.getEntityCache().get(ProcessDescriptor.class, id);
		ProcessNode processNode = new ProcessNode(descriptor);
		return processNode;
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
			if (outNode == null || inNode == null)
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

	private GraphConfig createGraphConfig() {
		GraphConfig conf = new GraphConfig(getGraphicalViewer());
		conf.actionRegistry = getActionRegistry();
		conf.commandStack = getCommandStack();
		conf.model = model;
		return conf;
	}

	@Override
	protected void configureGraphicalViewer() {
		model = createModel();
		super.configureGraphicalViewer();
		config = createGraphConfig();
		config.configureGraphicalViewer();
		actionIds = config.configureActions();
		config.configureKeyHandler();
		config.configureContextMenu();
	}

	@Override
	protected void initializeGraphicalViewer() {
		config.initializeGraphicalViewer();
		config.configureZoomManager();
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		systemEditor.doSave(monitor);
	}

	public void updateModel(final IProgressMonitor monitor) {
		monitor.beginTask(M.UpdatingProductSystem, IProgressMonitor.UNKNOWN);
		systemEditor.updateModel();
		monitor.done();
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class type) {
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
		CommandStack stack = super.getCommandStack();
		if (stack == null)
			stack = new CommandStack();
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

	public void expand() {
		model = expandModel();
		if (getGraphicalViewer() != null) {
			getGraphicalViewer().deselectAll();
			getGraphicalViewer().setContents(model);
			for (ProcessNode node : model.getChildren()) {
				node.expandLeft();
				node.expandRight();
			}
		}
	}

	public void collapse() {
		model = createModel();
		if (getGraphicalViewer() != null) {
			getGraphicalViewer().deselectAll();
			getGraphicalViewer().setContents(model);
		}
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		this.selection = selection;
		updateActions(actionIds);
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
		ConnectionRouter router = ConnectionRouter.NULL;
		if (routed)
			router = TreeConnectionRouter.instance;
		for (ProcessNode node : model.getChildren())
			for (Link link : node.links)
				link.figure.setConnectionRouter(router);
	}

}
