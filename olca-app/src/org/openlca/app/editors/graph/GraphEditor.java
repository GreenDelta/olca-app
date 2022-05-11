package org.openlca.app.editors.graph;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.*;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.graph.edit.GraphEditPartFactory;
import org.openlca.app.editors.graph.model.GraphModel;
import org.openlca.app.editors.graph.model.IOPanel;
import org.openlca.app.editors.graph.model.Node;
import org.openlca.app.editors.graphical.GraphicalEditorInput;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;

import java.util.EventObject;

public class GraphEditor extends GraphicalEditor {


	private final ProductSystemEditor systemEditor;
	private GraphModel graphModel = new GraphModel();

	public static final double[] ZOOM_LEVELS = new double[] {
		0.01, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0 };

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput ginp) {
			if (ginp.getDescriptor() != null) {
				setPartName(Labels.name(ginp.getDescriptor()));
			}
		}
		super.init(site, input);
	}

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();

		// TODO
		//		GraphDropListener.on(this, viewer);
		//		viewer.getEditDomain().setActiveTool(
		//			new PanningSelectionTool());

		viewer.setContents(getGraphModel());
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();
		var viewer = getGraphicalViewer();

		ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();

		// set clipping strategy for connection layer
		ConnectionLayer connectionLayer = (ConnectionLayer) root
			.getLayer(LayerConstants.CONNECTION_LAYER);
		connectionLayer
			.setClippingStrategy(new ViewportAwareConnectionLayerClippingStrategy(
				connectionLayer));

		var zoom = root.getZoomManager();
		zoom.setZoomLevels(ZOOM_LEVELS);
		zoom.setZoomAnimationStyle(ZoomManager.ANIMATE_ZOOM_IN_OUT);
		viewer.setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
			MouseWheelZoomHandler.SINGLETON);

		var keyHandler = new KeyHandler();
		IAction zoomIn = new ZoomInAction(root.getZoomManager());
		IAction zoomOut = new ZoomOutAction(root.getZoomManager());
		getActionRegistry().registerAction(zoomIn);
		getActionRegistry().registerAction(zoomOut);
		// TODO Add delete action
//		keyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), delete);
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0), zoomIn);
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0), zoomOut);
		viewer.setKeyHandler(keyHandler);

		viewer.setRootEditPart(root);
		viewer.setEditPartFactory(new GraphEditPartFactory());

		loadProperties();

		// TODO Might some activate control listener.
	}

	protected void loadProperties() {
		// Zoom
		ZoomManager manager = (ZoomManager) getGraphicalViewer().getProperty(
			ZoomManager.class.toString());
		if (manager != null)
			manager.setZoom(getGraphModel().getZoom());
		// Scroll-wheel Zoom
		getGraphicalViewer().setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1),
			MouseWheelZoomHandler.SINGLETON);
	}

	@Override
	protected void setInput(IEditorInput input) {
		var node = new Node("Node 1");
		node.addChild(new IOPanel(true));
		graphModel.addChild(node);
		graphModel.addChild(new Node("Node 2"));
		graphModel.addChild(new Node("Node 3"));
		graphModel.addChild(new Node("Node 4"));
		setPartName("Testing");
	}

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	protected GraphModel getGraphModel() {
		return graphModel;
	}

	public void setGraphModel(GraphModel model) {
		graphModel = model;
	}

	public ZoomManager getZoomManager() {
		return getRootEditPart().getZoomManager();
	}

	private ScalableFreeformRootEditPart getRootEditPart() {
		return (ScalableFreeformRootEditPart) getGraphicalViewer().getRootEditPart();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	public Object getAdapter(Class type) {
		if (type == ZoomManager.class)
			return ((ScalableFreeformRootEditPart) getGraphicalViewer()
				.getRootEditPart())
				.getZoomManager();

		return super.getAdapter(type);
	}
}
