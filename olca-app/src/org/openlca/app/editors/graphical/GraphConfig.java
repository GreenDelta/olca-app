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
import org.eclipse.ui.IWorkbenchPart;
import org.openlca.app.components.ModelTransfer;
import org.openlca.app.editors.graphical.action.AddFlowAction;
import org.openlca.app.editors.graphical.action.AddProcessAction;
import org.openlca.app.editors.graphical.action.BuildSupplyChainMenuAction;
import org.openlca.app.editors.graphical.action.GraphActions;
import org.openlca.app.editors.graphical.action.LayoutMenuAction;
import org.openlca.app.editors.graphical.action.MarkingAction;
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
		var transfer = ModelTransfer.getInstance();
		var dropTarget = new DropTarget(viewer.getControl(),
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
		var editor = model.editor;
		var delete = new DeleteAction((IWorkbenchPart) editor) {
			@Override
			protected ISelection getSelection() {
				return editor.getSite()
						.getWorkbenchWindow()
						.getSelectionService()
						.getSelection();
			}
		};

		var actions = new IAction[] {
				new AddProcessAction(editor),
				new BuildSupplyChainMenuAction(editor),
				GraphActions.removeSupplyChain(editor),
				GraphActions.removeAllConnections(editor),
				MarkingAction.forMarking(editor),
				MarkingAction.forUnmarking(editor),
				GraphActions.saveImage(editor),
				GraphActions.expandAll(editor),
				GraphActions.collapseAll(editor),
				GraphActions.maximizeAll(editor),
				GraphActions.minimizeAll(editor),
				new LayoutMenuAction(editor),
				GraphActions.searchProviders(editor),
				GraphActions.searchRecipients(editor),
				GraphActions.open(editor),
				GraphActions.openMiniatureView(editor),
				GraphActions.showOutline(),
				new ZoomInAction(getZoomManager()),
				new ZoomOutAction(getZoomManager()),
				delete,
		};

		var ids = new ArrayList<String>();
		for (var action : actions) {
			this.actions.registerAction(action);
			ids.add(action.getId());
		}
		return ids;
	}

	void configureZoomManager() {
		getZoomManager().setZoomLevels(ZOOM_LEVELS);
		getZoomManager().setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(
				MouseWheelHandler.KeyGenerator.getKey(SWT.NONE), 
				MouseWheelZoomHandler.SINGLETON);
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

	private ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableRootEditPart getRootEditPart() {
		return (ScalableRootEditPart) viewer.getRootEditPart();
	}

}