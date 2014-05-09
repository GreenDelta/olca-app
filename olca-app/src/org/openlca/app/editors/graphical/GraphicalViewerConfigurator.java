package org.openlca.app.editors.graphical;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchPart;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.action.ActionFactory;
import org.openlca.app.editors.graphical.action.ActionIds;
import org.openlca.app.editors.graphical.model.AppEditPartFactory;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class GraphicalViewerConfigurator {

	public static final double[] ZOOM_LEVELS = new double[] { 0.01, 0.1, 0.25,
			0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0 };

	private GraphicalViewer viewer;
	private ActionRegistry actionRegistry;
	private CommandStack commandStack;
	private ProductSystemNode model;

	GraphicalViewerConfigurator(GraphicalViewer viewer) {
		this.viewer = viewer;
	}

	void setActionRegistry(ActionRegistry actionRegistry) {
		this.actionRegistry = actionRegistry;
	}

	void setCommandStack(CommandStack commandStack) {
		this.commandStack = commandStack;
	}

	void setModel(ProductSystemNode model) {
		this.model = model;
	}

	void initializeGraphicalViewer() {
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

		Transfer transferType = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(viewer.getControl(),
				DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(new Transfer[] { transferType });
		dropTarget.addDropListener(new GraphDropListener(model, transferType,
				commandStack));
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
		updateableActions.add(org.eclipse.ui.actions.ActionFactory.DELETE
				.getId());
		updateableActions.add(ActionIds.OPEN);
		updateableActions.add(ActionIds.MARK);
		updateableActions.add(ActionIds.UNMARK);
		updateableActions.add(ActionIds.SEARCH_PROVIDERS);
		updateableActions.add(ActionIds.SEARCH_RECIPIENTS);
		updateableActions.add(ActionIds.OPEN_MINIATURE_VIEW);
		return updateableActions;
	}

	private void registerStaticActions() {
		final ProductSystemGraphEditor editor = model.getEditor();
		actionRegistry.registerAction(ActionFactory
				.createBuildSupplyChainMenuAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createRemoveSupplyChainAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createRemoveAllConnectionsAction(editor));
		actionRegistry.registerAction(ActionFactory.createMarkAction(editor));
		actionRegistry.registerAction(ActionFactory.createUnmarkAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createSaveImageAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createExpandAllAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createCollapseAllAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createMaximizeAllAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createMinimizeAllAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createLayoutMenuAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createSearchProvidersAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createSearchRecipientsAction(editor));
		actionRegistry.registerAction(ActionFactory.createOpenAction(editor));
		actionRegistry.registerAction(ActionFactory
				.createOpenMiniatureViewAction(editor));
		actionRegistry.registerAction(new ZoomInAction(getZoomManager()));
		actionRegistry.registerAction(new ZoomOutAction(getZoomManager()));

		DeleteAction delAction = new DeleteAction((IWorkbenchPart) editor) {
			@Override
			protected ISelection getSelection() {
				return editor.getSite().getWorkbenchWindow()
						.getSelectionService().getSelection();
			}
		};
		actionRegistry.registerAction(delAction);
	}

	void configureZoomManager() {
		getZoomManager().setZoomLevels(ZOOM_LEVELS);
		getZoomManager().setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);
	}

	void configureKeyHandler() {
		KeyHandler keyHandler = new KeyHandler();
		keyHandler
				.put(KeyStroke.getPressed(SWT.DEL, 127, 0), actionRegistry
						.getAction(org.eclipse.ui.actions.ActionFactory.DELETE
								.getId()));
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				actionRegistry.getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				actionRegistry.getAction(GEFActionConstants.ZOOM_OUT));
		viewer.setKeyHandler(keyHandler);
	}

	void configureContextMenu() {
		ContextMenuProvider provider = new MenuProvider(viewer, actionRegistry);
		viewer.setContextMenu(provider);
	}

	private ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) viewer.getRootEditPart();
	}

}