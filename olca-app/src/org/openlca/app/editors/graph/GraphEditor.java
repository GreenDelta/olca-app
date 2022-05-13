package org.openlca.app.editors.graph;

import java.util.EventObject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.*;
import org.eclipse.gef.*;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.*;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.openlca.app.editors.graph.edit.GraphEditPartFactory;
import org.openlca.app.editors.graph.model.Graph;
import org.openlca.app.editors.graph.model.GraphFactory;
import org.openlca.app.editors.graphical.GraphConfig;
import org.openlca.app.editors.systems.ProductSystemEditor;
import org.openlca.app.util.Labels;
import org.openlca.core.model.ProductSystem;

/**
 * A {@link GraphEditor} is the starting point of the graphical interface of a
 * product system. It creates an <code>Editor</code> containing a single
 * <code>GraphicalViewer</code> as its control.
 * The <code>GraphModel</code>  is the head of the model to be further
 * displayed.
 */
public class GraphEditor extends GraphicalEditor {

	private final ProductSystemEditor systemEditor;
	private Graph graph = new Graph();

	public static final double[] ZOOM_LEVELS = new double[] {
		0.01, 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0 };

	// TODO: save this in the same way like the layout is currently stored
	public final GraphConfig config = new GraphConfig();
	private final GraphFactory graphFactory = new GraphFactory(config);

	public GraphEditor(ProductSystemEditor editor) {
		this.systemEditor = editor;
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
		throws PartInitException {
		setEditDomain(new DefaultEditDomain(this));
		if (input instanceof GraphicalEditorInput graphInput) {
			if (graphInput.descriptor() != null) {
				setPartName(Labels.name(graphInput.descriptor()));
			}
		}
		super.init(site, input);
	}

	@Override
	protected void initializeGraphicalViewer() {
		var viewer = getGraphicalViewer();

		GraphDropListener.on(this);
		// TODO Implement a PanningSelectionTool without pressing SpaceBar and
		//  Selection while pressing Ctrl.
		viewer.getEditDomain().setActiveTool(new PanningSelectionTool());

		viewer.setContents(getModel());

		// TODO load the graph settings
		//		var fileApplied = GraphFile.apply(this);
		//		if (!fileApplied) {
		//			// no saved settings applied =>
		//			// try to find a good configuration
		//			var system = systemEditor.getModel();
		//			if (system.referenceProcess != null) {
		//				var refNode = model.getProcessNode(
		//					system.referenceProcess.id);
		//				if (refNode != null) {
		//					refNode.expandLeft();
		//					refNode.expandRight();
		//				}
		//			}
		//		}
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
			manager.setZoom(getModel().getZoom());
		// Scroll-wheel Zoom
		getGraphicalViewer().setProperty(
			MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1),
			MouseWheelZoomHandler.SINGLETON);
	}

	@Override
	protected void setInput(IEditorInput input) {
		//TODO
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	public void setDirty() {
		systemEditor.setDirty(true);
	}

	@Override
	public boolean isDirty() {
		return systemEditor.isDirty();
	}

	public ProductSystem getProductSystem() {
		return systemEditor.getModel();
	}

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY);
		super.commandStackChanged(event);
	}

	public Graph getModel() {
		return graph;
	}

	public GraphFactory getGraphFactory() {
		return graphFactory;
	}

	public void setGraphModel(Graph model) {
		graph = model;
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
