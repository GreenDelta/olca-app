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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.app.db.Cache;
import org.openlca.app.editors.graphical.layout.GraphLayoutType;
import org.openlca.app.editors.graphical.model.ConnectionLink;
import org.openlca.app.editors.graphical.model.ProcessNode;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.editors.graphical.model.TreeConnectionRouter;
import org.openlca.app.editors.graphical.outline.OutlinePage;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductSystemGraphEditor extends GraphicalEditor {

	public static final String ID = "editors.productsystem.graphical";

	private ProductSystem system;
	private ProductSystemEditor systemEditor;
	private ProductSystemNode model;
	private GraphLayoutType layoutType = GraphLayoutType.TREE_LAYOUT;
	private OutlinePage outline;
	private boolean routed;
	private GraphicalViewerConfigurator configurator;
	private ISelection selection;
	private List<String> actionIds;

	public ProductSystemGraphEditor(ProductSystem system,
			ProductSystemEditor editor) {
		this.system = system;
		this.systemEditor = editor;
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

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput) {
			GraphicalEditorInput modelInput = (GraphicalEditorInput) input;
			if (modelInput.getDescriptor() != null)
				setPartName(Labels.getDisplayName(modelInput.getDescriptor()));
		}
		super.init(site, input);
	}

	private ProductSystemNode createModel() {
		long referenceId = system.getReferenceProcess().getId();
		ProductSystemNode productSystemNode = new ProductSystemNode(system,
				this);
		productSystemNode.add(createProcessNode(referenceId));
		return productSystemNode;
	}

	private ProductSystemNode expandModel() {
		ProductSystemNode productSystemNode = new ProductSystemNode(system,
				this);
		for (Long id : system.getProcesses())
			productSystemNode.add(createProcessNode(id));
		return productSystemNode;
	}

	private ProcessNode createProcessNode(long id) {
		ProcessDescriptor descriptor = Cache.getEntityCache().get(
				ProcessDescriptor.class, id);
		ProcessNode processNode = new ProcessNode(descriptor);
		return processNode;
	}

	private GraphicalViewerConfigurator createGraphicalViewerConfigurator() {
		GraphicalViewerConfigurator configurator = new GraphicalViewerConfigurator(
				getGraphicalViewer());
		configurator.setActionRegistry(getActionRegistry());
		configurator.setCommandStack(getCommandStack());
		configurator.setModel(model);
		return configurator;
	}

	@Override
	protected void configureGraphicalViewer() {
		model = createModel();
		super.configureGraphicalViewer();
		configurator = createGraphicalViewerConfigurator();
		configurator.configureGraphicalViewer();
		actionIds = configurator.configureActions();
		configurator.configureZoomManager();
		configurator.configureKeyHandler();
		configurator.configureContextMenu();
	}

	@Override
	protected void initializeGraphicalViewer() {
		configurator.initializeGraphicalViewer();
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {
		// do nothing as this editor is embedded in a system editor
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class type) {
		if (type == ZoomManager.class) {
			return ((ScalableRootEditPart) getGraphicalViewer()
					.getRootEditPart()).getZoomManager();
		}
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

	public GraphLayoutType getLayoutType() {
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
			router = TreeConnectionRouter.get();
		for (ProcessNode node : model.getChildren())
			for (ConnectionLink link : node.getLinks())
				link.getFigure().setConnectionRouter(router);
	}

}
