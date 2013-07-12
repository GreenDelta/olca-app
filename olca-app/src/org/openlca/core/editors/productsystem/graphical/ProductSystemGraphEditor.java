/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.productsystem.graphical;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.ConnectionRouter;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ContentOutlinePage;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.openlca.core.application.Messages;
import org.openlca.core.application.actions.OpenEditorAction;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.ModelEditor;
import org.openlca.core.editors.productsystem.graphical.actions.BuildSupplyChainMenuAction;
import org.openlca.core.editors.productsystem.graphical.actions.GetLinksPopupAction;
import org.openlca.core.editors.productsystem.graphical.actions.HideShowAction;
import org.openlca.core.editors.productsystem.graphical.actions.MarkProcessAction;
import org.openlca.core.editors.productsystem.graphical.actions.ProcessCreateCommand;
import org.openlca.core.editors.productsystem.graphical.actions.RemoveConnectionsFromProcessAction;
import org.openlca.core.editors.productsystem.graphical.actions.RemoveSupplyChainAction;
import org.openlca.core.editors.productsystem.graphical.model.AppEditPartFactory;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLink;
import org.openlca.core.editors.productsystem.graphical.model.ConnectionLinkEditPart;
import org.openlca.core.editors.productsystem.graphical.model.ExchangeNode;
import org.openlca.core.editors.productsystem.graphical.model.Node;
import org.openlca.core.editors.productsystem.graphical.model.ProcessNode;
import org.openlca.core.editors.productsystem.graphical.model.ProcessPart;
import org.openlca.core.editors.productsystem.graphical.model.ProductSystemNode;
import org.openlca.core.editors.productsystem.graphical.outline.AppTreeEditPartFactory;
import org.openlca.core.editors.productsystem.graphical.outline.ProcessTreeEditPart;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.ui.dnd.ModelTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * 
 * The graphical editor. This class creates the product system model,
 * initializes the outline and the actions and configures the graphical viewer.
 * 
 * @see GraphicalEditor
 * 
 * @author Sebastian Greve
 */
public class ProductSystemGraphEditor extends GraphicalEditor implements
		PropertyChangeListener {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * ID of this editor
	 */
	public static final String ID = "editor.ProductSystemGraphEditor";

	/**
	 * Build supply chain pop up menu action
	 */
	private BuildSupplyChainMenuAction bscAction;

	/**
	 * Contains all processes which have already been created. Maps
	 * process.getKey() to the created ProcessNode
	 */
	private final Map<Long, ProcessNode> createdProcesses = new HashMap<>();

	/**
	 * The database
	 */
	private IDatabase database;

	/**
	 * The 'delete' action of the context menu. Used for enabling at selection
	 */
	private DeleteAction delAction;

	/**
	 * The parent model editor containing this editor as a page
	 */
	private final ModelEditor editor;

	/**
	 * Indicates if the complete system is displayed or not (only necessary
	 * while initializing the connection links first time) @see
	 * {@link #propertyChange(PropertyChangeEvent)}
	 */
	private boolean full = false;

	/**
	 * The 'get providers' action of the context menu
	 */
	private GetLinksPopupAction getProviderPopupAction;

	/**
	 * The 'get recipients' action of the context menu
	 */
	private GetLinksPopupAction getRecipientPopupAction;

	/**
	 * Hides a selected process node
	 */
	private HideShowAction hideAction;
	/**
	 * The actual layout type
	 */
	private final GraphLayoutType layoutType = GraphLayoutType.TreeLayout;

	/**
	 * Action to mark processes
	 */
	private MarkProcessAction markAction;

	/**
	 * The model which is contributed to the graphical viewer
	 */
	private ProductSystemNode model;

	/**
	 * The 'open process in editor' action of the context menu. Used for
	 * enabling at selection
	 */
	private OpenEditorAction openAction;

	/**
	 * The outline listing the processes
	 */
	private OutlinePage outline;

	/**
	 * The product system which this editor edits
	 */
	private ProductSystem productSystem;

	/**
	 * The 'remove connections from process' action of the context menu. Used
	 * for enabling at selection
	 */
	private RemoveConnectionsFromProcessAction removeAction;

	/**
	 * Indicates if the connections should be routed
	 */
	private boolean route = false;

	/**
	 * Action to remove the supply chain of a process
	 */
	private RemoveSupplyChainAction rscAction;

	/**
	 * Shows a selected process node
	 */
	private HideShowAction showAction;

	/**
	 * Action to unmark processes
	 */
	private MarkProcessAction unmarkAction;

	/**
	 * The zoom manager of the graphical editor
	 */
	private ZoomManager zoomManager;

	public ProductSystemGraphEditor(ModelEditor editor, IDatabase database,
			ProductSystem productSystem) {
		this.editor = editor;
		this.database = database;
		this.productSystem = productSystem;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (productSystem != null) {
			setPartName(productSystem.getName());
		}
		super.init(site, input);
	}

	/**
	 * 
	 * Creates the {@link ProductSystemNode} and paints and adds its
	 * {@link ProcessNode}s
	 * 
	 * @return ProductSystemNode - the model created from the given product
	 *         system
	 */
	private ProductSystemNode createProductSystem() {
		createdProcesses.clear();
		final ProductSystemNode productSystemNode = new ProductSystemNode(
				productSystem, this);
		if (full) {
			// paint each process
			for (final Process process : productSystem.getProcesses()) {
				productSystemNode.addChild(paintProcess(process));
			}
		} else {
			// only paint the reference process and it's providing processes
			productSystemNode.addChild(paintProcess(productSystem
					.getReferenceProcess()));
			for (final ProcessLink link : productSystem
					.getIncomingLinks(productSystem.getReferenceProcess()
							.getId())) {
				if (!createdProcesses.containsKey(link.getProviderProcess()
						.getId())) {
					productSystemNode.addChild(paintProcess(link
							.getProviderProcess()));
				}
			}
		}
		return productSystemNode;
	}

	/**
	 * 
	 * Creates a {@link ProcessNode} from the given process descriptor and adds
	 * it to {@link #createdProcesses}
	 * 
	 * @param process
	 *            - the descriptor of the process
	 * @return The created ProcessNode
	 */
	private ProcessNode paintProcess(final Process process) {
		final ProcessNode processNode = new ProcessNode(process, productSystem
				.getProcesses().size() > 20);
		createdProcesses.put(process.getId(), processNode);
		return processNode;
	}

	/**
	 * Refreshes the delete action
	 * 
	 * @param selection
	 *            The current selection
	 */
	private void refreshDeleteAction(final IStructuredSelection selection) {
		if (delAction != null) {
			boolean enabled = true;
			int i = 0;
			final Object[] sel = selection.toArray();
			while (enabled && i < sel.length) {
				if (sel[i] instanceof ProcessPart) {
					final ProcessNode processNode = (ProcessNode) ((ProcessPart) sel[i])
							.getModel();
					if (processNode != null && processNode.getProcess() != null) {
						if (processNode.getProcess().getId() != productSystem
								.getReferenceProcess().getId()) {
							final ExchangeNode[] exchangeNodes = processNode
									.getExchangeNodes();
							boolean connected = false;
							int j = 0;
							// check if the process is linked to another
							while (!connected && j < exchangeNodes.length) {
								if (exchangeNodes[j].getLinks().size() > 0) {
									connected = true;
								} else {
									j++;
								}
							}
							// if one selected process is connected through an
							// exchange
							// node it cannot be deleted
							enabled = !connected;
						} else {
							// the reference process cannot be deleted
							enabled = false;
						}
					} else {
						enabled = false;
					}
				} else if (!(sel[i] instanceof ConnectionLinkEditPart)) {
					// if the product system node or anything else is selected
					// it
					// cannot be deleted, only process nodes and connection
					// links
					// can be deleted
					enabled = false;
				}
				i++;
			}
			delAction.setEnabled(enabled);
		}
	}

	/**
	 * Refreshes the 'get recipient' and 'get provider' action
	 * 
	 * @param selection
	 *            The current selection
	 */
	private void refreshGetLinksAction(final IStructuredSelection selection) {
		if (getProviderPopupAction != null && getRecipientPopupAction != null) {
			if (selection.toArray().length != 1) {
				// more than one node is selected
				getProviderPopupAction.setEnabled(false);
				getRecipientPopupAction.setEnabled(false);
			} else {
				if (selection.getFirstElement() instanceof ProcessPart) {
					// only process nodes can be applied
					final ProcessNode node = (ProcessNode) ((ProcessPart) selection
							.getFirstElement()).getModel();
					getProviderPopupAction.setProcessNode(node);
					getRecipientPopupAction.setProcessNode(node);
					// check if there are recipient exchanges
					getProviderPopupAction.setEnabled(node.getProcess()
							.getInputs().length > 0);
					// check if there are providing exchanges
					getRecipientPopupAction.setEnabled(node.getProcess()
							.getOutputs().length > 0);
				} else {
					getProviderPopupAction.setEnabled(false);
					getRecipientPopupAction.setEnabled(false);
				}
			}
		}
	}

	/**
	 * Refreshes the mark/unmark action
	 * 
	 * @param nodes
	 *            The current selection (process nodes only)
	 */
	private void refreshMarkActions(final ProcessNode[] nodes) {
		if (markAction != null && unmarkAction != null) {
			if (nodes.length > 0) {
				final ProductSystem productSystem = ((ProductSystemNode) nodes[0]
						.getParent()).getProductSystem();
				final boolean mark = false;
				// !productSystem.isMarked(nodes[0]
				// .getProcess().getId());
				if (mark) {
					markAction.setNodes(nodes);
				} else {
					unmarkAction.setNodes(nodes);
				}
				markAction.setEnabled(mark);
				unmarkAction.setEnabled(!mark);
			} else {
				markAction.setEnabled(false);
				unmarkAction.setEnabled(false);
			}
		}
	}

	/**
	 * Refreshes the open action
	 * 
	 * @param nodes
	 *            The current selected process nodes
	 */
	private void refreshOpenAction(final ProcessNode[] nodes) {
		if (openAction != null) {
			openAction.setModelComponent(database, null);
			if (nodes.length > 0) {
				// one or more nodes are selected
				final List<Process> processes = new ArrayList<>();
				for (final ProcessNode node : nodes) {
					processes.add(node.getProcess());
				}
				openAction.setModelComponents(database,
						processes.toArray(new Process[processes.size()]));
				openAction.setEnabled(true);
			} else {
				openAction.setEnabled(false);
			}
		}
	}

	/**
	 * Refresh the 'remove connection links' action
	 * 
	 * @param selection
	 *            The current selection
	 * @param nodes
	 *            The current selected process nodes
	 */
	private void refreshRemoveConnectionLinksAction(
			final IStructuredSelection selection, final ProcessNode[] nodes) {
		if (removeAction != null) {
			if (nodes.length == 0) {
				// no process node is selected
				removeAction.setEnabled(false);
			} else {
				// one or more process nodes are selected and their links can be
				// removed
				removeAction.setProcessNodes(nodes);
				removeAction.setSelection(selection);
				boolean hasLink = false;
				for (ProcessNode node : nodes) {
					if (node.hasConnections()) {
						hasLink = true;
						break;
					}
				}
				removeAction.setEnabled(hasLink);
			}
		}
	}

	/**
	 * Refreshes the build supply chain and remove supply chain actions
	 * 
	 * @param node
	 *            The current selected process node (or first element in the
	 *            selection)
	 */
	private void refreshSupplyActions(final ProcessNode node) {
		if (bscAction != null && rscAction != null) {
			if (node != null) {
				bscAction.setStartProcess(node.getProcess());
				rscAction.setProcessNode(node);
			}
			bscAction.setEnabled(node != null);
			rscAction.setEnabled(node != null && node.hasConnections());
		}
	}

	@Override
	protected void configureGraphicalViewer() {
		ArrayList<String> zoomContributions;

		full = productSystem.getProcesses().length <= 250;
		model = createProductSystem();
		route = productSystem.getProcessLinks().length <= 250;

		// configure viewer
		super.configureGraphicalViewer();
		final GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new AppEditPartFactory());
		final ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
		viewer.setRootEditPart(rootEditPart);

		// get zoom manager
		zoomManager = rootEditPart.getZoomManager();

		// initialize actions
		openAction = new OpenEditorAction();
		markAction = new MarkProcessAction(true);
		unmarkAction = new MarkProcessAction(false);
		getProviderPopupAction = new GetLinksPopupAction(true, database);
		getRecipientPopupAction = new GetLinksPopupAction(false, database);
		bscAction = new BuildSupplyChainMenuAction();
		bscAction.setProductSystemNode(model);
		rscAction = new RemoveSupplyChainAction();
		getActionRegistry().registerAction(openAction);
		getActionRegistry().registerAction(markAction);
		getActionRegistry().registerAction(unmarkAction);
		getActionRegistry().registerAction(getProviderPopupAction);
		getActionRegistry().registerAction(getRecipientPopupAction);
		getActionRegistry().registerAction(bscAction);
		getActionRegistry().registerAction(rscAction);
		removeAction = new RemoveConnectionsFromProcessAction();
		removeAction.addPropertyChangeListener(this);
		getActionRegistry().registerAction(removeAction);
		getActionRegistry().registerAction(new ZoomInAction(zoomManager));
		getActionRegistry().registerAction(new ZoomOutAction(zoomManager));

		delAction = new DeleteAction((IWorkbenchPart) this) {

			@Override
			protected ISelection getSelection() {
				return getSite().getWorkbenchWindow().getSelectionService()
						.getSelection();
			}

		};
		getActionRegistry().registerAction(delAction);

		zoomContributions = new ArrayList<>();
		zoomContributions.add(ZoomManager.FIT_ALL);
		zoomContributions.add(ZoomManager.FIT_HEIGHT);
		zoomContributions.add(ZoomManager.FIT_WIDTH);
		zoomManager.setZoomLevelContributions(zoomContributions);

		// initialize short cuts
		final KeyHandler keyHandler = new KeyHandler();
		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0),
				getActionRegistry().getAction(ActionFactory.DELETE.getId()));
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);
		viewer.setKeyHandler(keyHandler);
		final ContextMenuProvider provider = new AppContextMenuProvider(viewer,
				getActionRegistry());
		viewer.setContextMenu(provider);
	}

	@Override
	protected void initializeGraphicalViewer() {
		final GraphicalViewer viewer = getGraphicalViewer();
		viewer.setRootEditPart(new ScalableRootEditPart() {

			@Override
			protected LayeredPane createPrintableLayers() {
				final LayeredPane pane = new LayeredPane();

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

		double[] zoomLevels = new double[] { 0.005, 0.01, 0.02, 0.0375, 0.075,
				0.125, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0,
				10.0, 20.0, 40.0, 80.0, 150.0, 300.0, 500.0, 1000.0 };
		((ScalableRootEditPart) getGraphicalViewer().getRootEditPart())
				.getZoomManager().setZoomLevels(zoomLevels);

		final Transfer transferType = ModelTransfer.getInstance();
		final DropTarget dropTarget = new DropTarget(viewer.getControl(),
				DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		dropTarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void drop(final DropTargetEvent event) {
				if (transferType.isSupportedType(event.currentDataType)
						&& event.data != null
						&& event.data instanceof Object[]
						&& ((Object[]) event.data)[0].getClass() == Process.class) {
					final Object[] data = (Object[]) event.data;
					if (data[data.length - 1] instanceof String
							&& data[data.length - 1].toString().equals(
									database.getUrl())) {
						final IModelComponent[] components = new IModelComponent[data.length - 1];
						for (int i = 0; i < data.length - 1; i++) {
							components[i] = (IModelComponent) data[i];
						}
						Command command = null;
						for (final IModelComponent component : components) {
							try {
								final Process process = database.select(
										Process.class, component.getId());
								final ProcessCreateCommand cmd = new ProcessCreateCommand();
								cmd.setProductSystemNode(model);
								final ProcessNode processNode = new ProcessNode(
										process, false);
								cmd.setProcessNode(processNode);
								if (command == null) {
									command = cmd;
								} else {
									command = command.chain(cmd);
								}
							} catch (final Exception e) {
								log.error("Drop failed", e);
							}
						}
						if (command != null && command.canExecute()) {
							getCommandStack().execute(command);
						}
					}
				}
			}
		});
		viewer.setContents(model);
	}

	@Override
	public void dispose() {
		if (productSystem != null) {
			productSystem.removePropertyChangeListener(this);
		}
		productSystem = null;
		if (createdProcesses != null) {
			createdProcesses.clear();
		}
		if (removeAction != null) {
			removeAction.dispose();
		}
		if (getProviderPopupAction != null) {
			getProviderPopupAction.dispose();
		}
		if (getRecipientPopupAction != null) {
			getRecipientPopupAction.dispose();
		}
		if (model != null) {
			model.dispose();
		}
		super.dispose();
	}

	@Override
	public void doSave(final IProgressMonitor monitor) {

	}

	@Override
	@SuppressWarnings("rawtypes")
	public Object getAdapter(final Class type) {
		if (type == ZoomManager.class) {
			return ((ScalableRootEditPart) getGraphicalViewer()
					.getRootEditPart()).getZoomManager();
		}
		if (type == IContentOutlinePage.class) {
			outline = new OutlinePage();
			return outline;
		}
		return super.getAdapter(type);
	}

	@Override
	public CommandStack getCommandStack() {
		CommandStack cs = null;
		if (super.getCommandStack() != null) {
			cs = super.getCommandStack();
		} else {
			cs = new CommandStack();
		}
		return cs;
	}

	/**
	 * Getter of the database
	 * 
	 * @return The database
	 */
	public IDatabase getDatabase() {
		return database;
	}

	/**
	 * Getter of the model editor
	 * 
	 * @return The parent model editor containing this editor as a page
	 */
	public ModelEditor getEditor() {
		return editor;
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	/**
	 * Getter of the layoutType-field
	 * 
	 * @return The actual layout type
	 */
	public GraphLayoutType getLayoutType() {
		return layoutType;
	}

	/**
	 * Getter of the model-field
	 * 
	 * @return The actual product system node
	 */
	public ProductSystemNode getModel() {
		return model;
	}

	/**
	 * Getter of the outline
	 * 
	 * @return The outline listing the processes
	 */
	public OutlinePage getOutline() {
		return outline;
	}

	/**
	 * Getter of the viewable area
	 * 
	 * @return The client are of the figure of the root edit part of the
	 *         graphical viewer
	 */
	public Rectangle getViewableArea() {
		return ((Viewport) ((ScalableRootEditPart) getGraphicalViewer()
				.getRootEditPart()).getFigure()).getClientArea();
	}

	/**
	 * Getter of the zoomManager-field
	 * 
	 * @return The zoom manager of the graphical editor
	 */
	public ZoomManager getZoomManager() {
		return zoomManager;
	}

	/**
	 * Getter of route
	 * 
	 * @return True if the connections should be routed, false otherwise
	 */
	public boolean isRoute() {
		return route;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("firstTimeInitialized")) {
			ProcessLink[] links = null;
			if (full) {
				// set all process nodes visible
				for (final Object o : model.getChildrenArray()) {
					if (o instanceof ProcessNode) {
						final ProcessNode processNode = (ProcessNode) o;
						processNode.getFigure().setVisibility(true);
						processNode.getFigure().setVisible(true);
						processNode.getFigure().setExpandedLeft(true);
						processNode.getFigure().setExpandedRight(true);
					}
				}
				links = productSystem.getProcessLinks();
			} else {
				links = productSystem.getIncomingLinks(productSystem
						.getReferenceProcess().getId());
			}
			for (final ProcessLink processLink : links) {
				// create the links
				final ProcessNode sourceNode = createdProcesses.get(processLink
						.getProviderProcess().getId());
				final ProcessNode targetNode = createdProcesses.get(processLink
						.getRecipientProcess().getId());
				final ConnectionLink link = new ConnectionLink(
						sourceNode.getExchangeNode(processLink
								.getProviderOutput().getId()),
						targetNode.getExchangeNode(processLink
								.getRecipientInput().getId()), processLink);
				link.link();
			}

		} else if (evt.getPropertyName().equals("RemovedConnections")) {
			// if connections are removed from a process node it no can be
			// deleted
			refreshDeleteAction((IStructuredSelection) evt.getNewValue());
		}
	}

	/**
	 * Resets the graphical viewer content
	 * 
	 * @param full
	 *            Indicates if the full system should be displayed or only a
	 *            minimal (reference process and first level of suppliers), if
	 *            null the last value will be used
	 */
	public void reset(final Boolean full) {
		if (full != null) {
			this.full = full;
		}
		model = createProductSystem();
		if (getGraphicalViewer() != null) {
			getGraphicalViewer().deselectAll();
			getGraphicalViewer().setContents(model);
		}
		if (showAction != null) {
			showAction.setProductSystemNode(model);
		}
		if (hideAction != null) {
			hideAction.setProductSystemNode(model);
		}
		if (bscAction != null) {
			bscAction.setProductSystemNode(model);
		}
	}

	@Override
	public void selectionChanged(final IWorkbenchPart part,
			final ISelection selection) {
		super.selectionChanged(part, selection);
		final List<ProcessNode> processNodes = new ArrayList<>();
		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			// filter process nodes only
			final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (final Object o : structuredSelection.toArray()) {
				if (o instanceof ProcessPart) {
					processNodes
							.add((ProcessNode) ((ProcessPart) o).getModel());
				}
			}
		}
		final ProcessNode[] nodes = processNodes
				.toArray(new ProcessNode[processNodes.size()]);

		// refresh the actions with the new selection
		refreshOpenAction(nodes);
		refreshMarkActions(nodes);
		if (processNodes.size() == 1) {
			refreshSupplyActions(processNodes.get(0));
		} else {
			refreshSupplyActions(null);
		}
		if (selection instanceof IStructuredSelection) {
			refreshRemoveConnectionLinksAction(
					(IStructuredSelection) selection,
					processNodes.toArray(new ProcessNode[processNodes.size()]));
		} else {
			refreshRemoveConnectionLinksAction(new StructuredSelection(),
					new ProcessNode[0]);
		}
		if (selection instanceof IStructuredSelection) {
			refreshDeleteAction((IStructuredSelection) selection);
			refreshGetLinksAction((IStructuredSelection) selection);
		}
	}

	@Override
	public void setFocus() {
		if (getGraphicalViewer() != null
				&& getGraphicalViewer().getControl() != null) {
			super.setFocus();
		}
	}

	/**
	 * Setter of route
	 * 
	 * @param route
	 *            Indicates if the connections should be routed
	 */
	public void setRoute(final boolean route) {
		this.route = route;
		// for each process node
		for (final Node node : model.getChildrenArray()) {
			if (node instanceof ProcessNode) {
				// for each exchange node
				for (final ExchangeNode eNode : ((ProcessNode) node)
						.getExchangeNodes()) {
					// for each connection link
					for (final ConnectionLink link : eNode.getLinks()) {
						((Connection) link.getFigure())
								.setConnectionRouter(route ? new TreeConnectionRouter()
										: ConnectionRouter.NULL);
					}
				}
			}
		}
	}

	/**
	 * 
	 * The outline page for the graphical editor. Lists all processes and allows
	 * searching for specific processes
	 * 
	 * @see ContentOutlinePage
	 * 
	 * @author Sebastian Greve
	 */
	public class OutlinePage extends ContentOutlinePage implements
			PropertyChangeListener {

		/**
		 * The sash form containing the table and the search text field
		 */
		private SashForm sash;

		/**
		 * Creates a new instance
		 */
		public OutlinePage() {
			super(new TreeViewer());
		}

		/***
		 * 
		 * Creates the context menu of the outline
		 * 
		 * @return {@link MenuManager}
		 */
		private MenuManager createContextMenu() {
			final MenuManager menuManager = new MenuManager();
			showAction = new HideShowAction((TreeViewer) getViewer(), model,
					true);
			hideAction = new HideShowAction((TreeViewer) getViewer(), model,
					false);
			menuManager.add(showAction);
			menuManager.add(hideAction);
			return menuManager;
		}

		/***
		 * 
		 * Checks if 'compareWord' matches the search string 'word'
		 * 
		 * @param phrase
		 *            a search phrase (can contain * as placeholder)
		 * @param compareTo
		 *            The word which is compared to
		 * @return true if compareWord contains all part strings between the *
		 *         characters, else false
		 */
		private boolean matches(final String phrase, final String compareTo) {
			boolean matches = true;
			String compareWord = compareTo;
			String word = phrase;
			boolean fromBeginning = true;
			if (compareWord.startsWith("*")) {
				fromBeginning = false;
				while (compareWord.charAt(0) == '*' && compareWord.length() > 1) {
					compareWord = compareWord.substring(1);
				}
			}
			final List<String> words = new ArrayList<>();
			while (compareWord.endsWith("*")) {
				compareWord = compareWord
						.substring(0, compareWord.length() - 1);
			}
			while (compareWord.contains("*")) {
				final String newSubWord = compareWord.substring(0,
						compareWord.indexOf('*'));
				compareWord = compareWord
						.substring(compareWord.indexOf('*') + 1);
				words.add(newSubWord);
			}
			if (compareWord.length() > 0) {
				words.add(compareWord);
			}
			if (words.size() == 0) {
				matches = false;
			}
			if (matches) {
				int startIndex = 0;
				if (fromBeginning) {
					if (!word.startsWith(words.get(0))) {
						matches = false;
					}
					word = word.substring(word.indexOf(words.get(0))
							+ words.get(0).length());
					startIndex = 1;
				}
				if (matches) {
					int i = startIndex;
					while (i < words.size() && matches) {
						if (!word.contains(words.get(i))) {
							matches = false;
						} else {
							word = word.substring(word.indexOf(words.get(i))
									+ words.get(i).length());
							i++;
						}
					}
				}
			}
			return matches;
		}

		@Override
		public void createControl(final Composite parent) {

			sash = new SashForm(parent, SWT.VERTICAL);
			final Label label = new Label(sash, SWT.BORDER_SOLID);
			label.setText(Messages.Systems_ProductSystemGraphEditor_FilterLabel);
			final Text text = new Text(sash, SWT.BORDER_SOLID);
			text.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(final PaintEvent e) {
					if (sash.getSize().y >= 30) {
						final int[] weights = sash.getWeights();
						weights[0] = 18;
						weights[1] = 18;
						weights[2] = sash.getSize().y - 30;
						sash.setWeights(weights);
					}
				}

			});
			getViewer().createControl(sash);
			getViewer().setEditDomain(getEditDomain());
			getViewer().setEditPartFactory(new AppTreeEditPartFactory(model));
			getViewer().setContents(model.getProductSystem());
			getViewer().setContextMenu(createContextMenu());
			getSelectionSynchronizer().addViewer(getViewer());
			text.addModifyListener(new ModifyListener() {

				@Override
				public void modifyText(final ModifyEvent e) {
					final List<ProcessTreeEditPart> selection = new ArrayList<>();
					if (text.getText() == null || text.getText().length() == 0) {
						getViewer().deselectAll();
					} else {
						for (final Object o : getViewer().getContents()
								.getChildren()) {
							if (o instanceof ProcessTreeEditPart) {
								if (text.getText().equals("*")
										|| matches(
												((Process) ((ProcessTreeEditPart) o)
														.getModel()).getName()
														.toLowerCase(), text
														.getText()
														.toLowerCase())) {
									selection.add((ProcessTreeEditPart) o);
								}
							}
						}
					}
					if (selection.size() == 0) {
						getViewer().deselectAll();
					} else {
						final ProcessTreeEditPart[] selectionArray = new ProcessTreeEditPart[selection
								.size()];
						selection.toArray(selectionArray);
						final StructuredSelection result = new StructuredSelection(
								selectionArray);
						getViewer().setSelection(result);
					}

				}

			});
			model.getProductSystem().addPropertyChangeListener(this);
			model.addPropertyChangeListener(this);
		}

		@Override
		public void dispose() {
			super.dispose();
			if (sash != null) {
				sash.dispose();
				sash = null;
			}
			getSelectionSynchronizer().removeViewer(getViewer());
			getViewer().setContents(null);
			getViewer().setEditDomain(null);
			getViewer().setEditPartFactory(null);
			if (model != null) {
				model.getProductSystem().removePropertyChangeListener(this);
			}
		}

		@Override
		public Control getControl() {
			return sash;
		}

		@Override
		public void propertyChange(final PropertyChangeEvent arg0) {
			if (arg0.getPropertyName().equals("processes")
					|| arg0.getPropertyName().equals("processLinks")
					|| arg0.getPropertyName().equals("refreshTree")) {
				getViewer().setEditPartFactory(
						new AppTreeEditPartFactory(model));
				getViewer().setContents(model.getProductSystem());
				getViewer().getControl().redraw();
				getViewer().getControl().update();
			}
		}

		/**
		 * Refreshes the outline
		 */
		public void refresh() {
			propertyChange(new PropertyChangeEvent(this, "refreshTree",
					"not null", null));
		}

		@Override
		public void setSelection(final ISelection selection) {
			super.setSelection(selection);
		}
	}

}
