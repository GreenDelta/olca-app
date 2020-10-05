package org.openlca.app.editors.graphical;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchPart;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.action.GraphActions;
import org.openlca.app.editors.graphical.action.ActionIds;
import org.openlca.app.editors.graphical.model.AppEditPartFactory;
import org.openlca.app.editors.graphical.model.ProductSystemNode;

public class GraphConfig {

	public static final double[] ZOOM_LEVELS = new double[] {
			0.01, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0 };

	private final GraphicalViewer viewer;

	ActionRegistry actions;
	CommandStack commandStack;
	ProductSystemNode model;

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
		Transfer transfer = ModelTransfer.getInstance();
		DropTarget dropTarget = new DropTarget(viewer.getControl(),
				DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT);
		dropTarget.setTransfer(transfer);
		dropTarget.addDropListener(new GraphDropListener(
				model, transfer, commandStack));
		viewer.getEditDomain().setActiveTool(
				new PanningSelectionTool());
		viewer.setContents(model);
	}

	void configureGraphicalViewer() {
		viewer.setEditPartFactory(new AppEditPartFactory());
		viewer.setRootEditPart(new ScalableRootEditPart());
	}

	List<String> configureActions() {
		registerStaticActions();
		var updateableActions = new ArrayList<String>();
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
		return updateableActions;
	}

	private void registerStaticActions() {
		var editor = model.editor;
		actions.registerAction(GraphActions.buildSupplyChainMenu(editor));
		actions.registerAction(GraphActions.removeSupplyChain(editor));
		actions.registerAction(GraphActions.removeAllConnections(editor));
		actions.registerAction(GraphActions.mark(editor));
		actions.registerAction(GraphActions.unmark(editor));
		actions.registerAction(GraphActions.saveImage(editor));
		actions.registerAction(GraphActions.expandAll(editor));
		actions.registerAction(GraphActions.collapseAll(editor));
		actions.registerAction(GraphActions.maximizeAll(editor));
		actions.registerAction(GraphActions.minimizeAll(editor));
		actions.registerAction(GraphActions.layoutMenu(editor));
		actions.registerAction(GraphActions.searchProviders(editor));
		actions.registerAction(GraphActions.searchRecipients(editor));
		actions.registerAction(GraphActions.open(editor));
		actions.registerAction(GraphActions.openMiniatureView(editor));
		actions.registerAction(GraphActions.showOutline());
		actions.registerAction(new ZoomInAction(getZoomManager()));
		actions.registerAction(new ZoomOutAction(getZoomManager()));
		DeleteAction delAction = new DeleteAction((IWorkbenchPart) editor) {
			@Override
			protected ISelection getSelection() {
				return editor.getSite().getWorkbenchWindow().getSelectionService().getSelection();
			}
		};
		actions.registerAction(delAction);
	}

	void configureZoomManager() {
		getZoomManager().setZoomLevels(ZOOM_LEVELS);
		getZoomManager().setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE), MouseWheelZoomHandler.SINGLETON);
	}

	void configureKeyHandler() {
		KeyHandler keyHandler = new KeyHandler();
		IAction delete = actions.getAction(org.eclipse.ui.actions.ActionFactory.DELETE.getId());
		IAction zoomIn = actions.getAction(GEFActionConstants.ZOOM_IN);
		IAction zoomOut = actions.getAction(GEFActionConstants.ZOOM_OUT);
		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), delete);
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), zoomIn);
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), zoomOut);
		viewer.setKeyHandler(keyHandler);
	}

	void configureContextMenu() {
		MenuProvider provider = new MenuProvider(viewer, actions);
		viewer.setContextMenu(provider);
	}

	private ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) viewer.getRootEditPart();
	}

}