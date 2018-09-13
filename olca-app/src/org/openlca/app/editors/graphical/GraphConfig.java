package org.openlca.app.editors.graphical;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchPart;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.action.ActionFactory;
import org.openlca.app.editors.graphical.action.ActionIds;
import org.openlca.app.editors.graphical.action.EditorAction;
import org.openlca.app.editors.graphical.model.AppEditPartFactory;
import org.openlca.app.editors.graphical.model.ProductSystemNode;
import org.openlca.app.rcp.RcpActivator;

public class GraphConfig {

	public static final double[] ZOOM_LEVELS = new double[] {
			0.01, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0 };

	private final GraphicalViewer viewer;

	ActionRegistry actionRegistry;
	CommandStack commandStack;
	ProductSystemNode model;

	private Collection<String> actionExtensionIds = new HashSet<>();

	GraphConfig(GraphicalViewer viewer) {
		this.viewer = viewer;
	}

	void initializeGraphicalViewer() {
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
		Transfer transferType = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(viewer.getControl(), DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		dropTarget.addDropListener(new GraphDropListener(model, transferType, commandStack));
		viewer.getEditDomain().setActiveTool(new PanningSelectionTool());
		viewer.setContents(model);
	}

	void configureGraphicalViewer() {
		viewer.setEditPartFactory(new AppEditPartFactory());
		ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
		viewer.setRootEditPart(rootEditPart);
	}

	List<String> configureActions() {
		registerStaticActions();
		List<String> updateableActions = new ArrayList<>();
		updateableActions.add(ActionIds.BUILD_SUPPLY_CHAIN_MENU);
		updateableActions.add(ActionIds.REMOVE_SUPPLY_CHAIN);
		updateableActions.add(ActionIds.REMOVE_ALL_CONNECTIONS);
		updateableActions.add(org.eclipse.ui.actions.ActionFactory.DELETE.getId());
		updateableActions.add(ActionIds.OPEN);
		updateableActions.add(ActionIds.MARK);
		updateableActions.add(ActionIds.UNMARK);
		updateableActions.add(ActionIds.SEARCH_PROVIDERS);
		updateableActions.add(ActionIds.SEARCH_RECIPIENTS);
		updateableActions.add(ActionIds.OPEN_MINIATURE_VIEW);
		List<String> updateableActionExtensions = configureActionExtensions();
		updateableActions.addAll(updateableActionExtensions);
		return updateableActions;
	}

	/**
	 * Get the action extension points and register them as actions in the
	 * graphical viewer
	 */
	private List<String> configureActionExtensions() {
		List<String> updateableActions = new ArrayList<>();
		List<Action> actions = loadActionExtensions();
		for (Action action : actions) {
			if (action instanceof EditorAction)
				((EditorAction) action).setEditor(model.editor);
			actionRegistry.registerAction(action);
			if (action instanceof UpdateAction)
				updateableActions.add(action.getId());
			actionExtensionIds.add(action.getId());
		}
		return updateableActions;
	}

	private void registerStaticActions() {
		final ProductSystemGraphEditor editor = model.editor;
		actionRegistry.registerAction(ActionFactory.buildSupplyChainMenu(editor));
		actionRegistry.registerAction(ActionFactory.removeSupplyChain(editor));
		actionRegistry.registerAction(ActionFactory.removeAllConnections(editor));
		actionRegistry.registerAction(ActionFactory.mark(editor));
		actionRegistry.registerAction(ActionFactory.unmark(editor));
		actionRegistry.registerAction(ActionFactory.saveImage(editor));
		actionRegistry.registerAction(ActionFactory.expandAll(editor));
		actionRegistry.registerAction(ActionFactory.collapseAll(editor));
		actionRegistry.registerAction(ActionFactory.maximizeAll(editor));
		actionRegistry.registerAction(ActionFactory.minimizeAll(editor));
		actionRegistry.registerAction(ActionFactory.layoutMenu(editor));
		actionRegistry.registerAction(ActionFactory.searchProviders(editor));
		actionRegistry.registerAction(ActionFactory.searchRecipients(editor));
		actionRegistry.registerAction(ActionFactory.open(editor));
		actionRegistry.registerAction(ActionFactory.openMiniatureView(editor));
		actionRegistry.registerAction(ActionFactory.showOutline());
		actionRegistry.registerAction(new ZoomInAction(getZoomManager()));
		actionRegistry.registerAction(new ZoomOutAction(getZoomManager()));
		DeleteAction delAction = new DeleteAction((IWorkbenchPart) editor) {
			@Override
			protected ISelection getSelection() {
				return editor.getSite().getWorkbenchWindow().getSelectionService().getSelection();
			}
		};
		actionRegistry.registerAction(delAction);
	}

	void configureZoomManager() {
		getZoomManager().setZoomLevels(ZOOM_LEVELS);
		getZoomManager().setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE), MouseWheelZoomHandler.SINGLETON);
	}

	void configureKeyHandler() {
		KeyHandler keyHandler = new KeyHandler();
		IAction delete = actionRegistry.getAction(org.eclipse.ui.actions.ActionFactory.DELETE.getId());
		IAction zoomIn = actionRegistry.getAction(GEFActionConstants.ZOOM_IN);
		IAction zoomOut = actionRegistry.getAction(GEFActionConstants.ZOOM_OUT);
		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), delete);
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), zoomIn);
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), zoomOut);
		viewer.setKeyHandler(keyHandler);
	}

	void configureContextMenu() {
		MenuProvider provider = new MenuProvider(viewer, actionRegistry);
		provider.setActionExtensions(actionExtensionIds);
		viewer.setContextMenu(provider);
	}

	private ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) viewer.getRootEditPart();
	}

	private List<Action> loadActionExtensions() {
		List<Action> adapters = new ArrayList<>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
				"org.openlca.app.editors.graphical.actions");
		for (IConfigurationElement element : elements) {
			Action action = loadAction(element);
			if (action == null)
				continue;
			adapters.add(action);
		}
		return adapters;
	}

	private Action loadAction(IConfigurationElement element) {
		try {
			return (Action) element.createExecutableExtension("class");
		} catch (ClassCastException | CoreException e) {
			IStatus status = new Status(IStatus.ERROR, RcpActivator.PLUGIN_ID,
					"Error while loading action extensions for graphical editor", e);
			RcpActivator.getDefault().getLog().log(status);
			return null;
		}
	}

}