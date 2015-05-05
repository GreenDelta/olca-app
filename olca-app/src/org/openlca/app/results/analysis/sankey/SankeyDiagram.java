package org.openlca.app.results.analysis.sankey;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.Layer;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.openlca.app.App;
import org.openlca.app.db.Cache;
import org.openlca.app.results.analysis.sankey.actions.SankeyMenu;
import org.openlca.app.results.analysis.sankey.model.ConnectionLink;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.matrix.ProcessLinkSearchMap;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.NwSetDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResultProvider;

public class SankeyDiagram extends GraphicalEditor implements
		PropertyChangeListener {

	public static final String ID = "editor.ProductSystemSankeyDiagram";

	private EntityCache cache = Cache.getEntityCache();
	private SankeyResult sankeyResult;
	private ProcessLinkSearchMap linkSearchMap;
	private Map<ProcessLink, ConnectionLink> createdLinks = new HashMap<>();
	private Map<Long, ProcessNode> createdProcesses = new HashMap<>();
	private ProductSystemNode systemNode;
	private ImpactMethodDescriptor method;
	private NwSetDescriptor nwSet;
	private ProductSystem productSystem;
	private FullResultProvider result;

	private double zoom = 1;

	public SankeyDiagram(CalculationSetup setUp, FullResultProvider result) {
		setEditDomain(new DefaultEditDomain(this));
		this.result = result;
		productSystem = setUp.getProductSystem();
		linkSearchMap = new ProcessLinkSearchMap(
				productSystem.getProcessLinks());
		sankeyResult = new SankeyResult(productSystem, result);
		method = setUp.getImpactMethod();
		nwSet = setUp.getNwSet();
		if (productSystem != null) {
			setPartName(productSystem.getName());
		}
	}

	public FullResultProvider getResult() {
		return result;
	}

	public ProcessLinkSearchMap getLinkSearchMap() {
		return linkSearchMap;
	}

	private void createConnections(long processId) {
		for (ProcessLink processLink : linkSearchMap
				.getIncomingLinks(processId)) {
			ProcessNode sourceNode = createdProcesses.get(processLink
					.getProviderId());
			ProcessNode targetNode = createdProcesses.get(processLink
					.getRecipientId());
			if (sourceNode != null && targetNode != null) {
				if (!createdLinks.containsKey(processLink)) {
					double ratio = sankeyResult
							.getLinkContribution(processLink);
					ConnectionLink link = new ConnectionLink(sourceNode,
							targetNode, processLink, ratio);
					createdLinks.put(processLink, link);
					createConnections(sourceNode.getProcess().getId());
				}
			}
		}
	}

	private ProcessNode createNode(ProcessDescriptor process) {
		ProcessNode node = new ProcessNode(process);
		long processId = process.getId();
		node.setSingleContribution(sankeyResult
				.getSingleContribution(processId));
		node.setSingleResult(sankeyResult.getSingleResult(processId));
		node.setTotalContribution(sankeyResult.getTotalContribution(processId));
		node.setTotalResult(sankeyResult.getTotalResult(processId));
		createdProcesses.put(process.getId(), node);
		return node;
	}

	/**
	 * Updates the connection links
	 */
	private void updateConnections() {
		createConnections(productSystem.getReferenceProcess().getId());
		for (final ConnectionLink link : createdLinks.values()) {
			link.link();
		}
	}

	private void updateModel(double cutoff) {
		if (cutoff == 0) {
			for (Long processId : productSystem.getProcesses()) {
				systemNode.addChild(createNode(cache.get(
						ProcessDescriptor.class, processId)));
			}
		} else {
			long refProcess = productSystem.getReferenceProcess().getId();
			Set<Long> processesToDraw = SankeyProcessList.calculate(
					sankeyResult, refProcess, cutoff, linkSearchMap);
			for (final Long processId : processesToDraw) {
				ProcessDescriptor process = cache.get(ProcessDescriptor.class,
						processId);
				ProcessNode node = createNode(process);
				systemNode.addChild(node);
			}
		}
	}

	@Override
	protected void configureGraphicalViewer() {
		ArrayList<String> zoomContributions;
		// configure viewer
		super.configureGraphicalViewer();

		MenuManager menu = SankeyMenu.create(this);
		getGraphicalViewer().setContextMenu(menu);

		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new SankeyEditPartFactory());
		ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
		viewer.setRootEditPart(rootEditPart);

		ZoomManager zoomManager = rootEditPart.getZoomManager();

		// append zoom actions to action registry
		getActionRegistry().registerAction(new ZoomInAction(zoomManager));
		getActionRegistry().registerAction(new ZoomOutAction(zoomManager));

		zoomContributions = new ArrayList<>();
		zoomContributions.add(ZoomManager.FIT_ALL);
		zoomContributions.add(ZoomManager.FIT_HEIGHT);
		zoomContributions.add(ZoomManager.FIT_WIDTH);
		zoomManager.setZoomLevelContributions(zoomContributions);

		// create key handler
		KeyHandler keyHandler = new KeyHandler();
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
		viewer.setKeyHandler(keyHandler);

		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);

	}

	@Override
	protected void initializeGraphicalViewer() {
		// create new root edit part with switched layers (connection layer
		// under the process layer)
		getGraphicalViewer().setRootEditPart(new ScalableRootEditPart() {

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
		// zoom listener
		((ScalableRootEditPart) getGraphicalViewer().getRootEditPart())
				.getZoomManager().addZoomListener(new ZoomListener() {

					@Override
					public void zoomChanged(final double arg0) {
						zoom = arg0;
					}
				});
		double[] zoomLevels = new double[] { 0.005, 0.01, 0.02, 0.0375, 0.075,
				0.125, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0, 4.0, 5.0,
				10.0, 20.0, 40.0, 80.0, 150.0, 300.0, 500.0, 1000.0 };
		((ScalableRootEditPart) getGraphicalViewer().getRootEditPart())
				.getZoomManager().setZoomLevels(zoomLevels);

		initContent();
	}

	// TODO: avoid double calculation here
	private void initContent() {
		Object defaultSelection = getDefaultSelection();
		if (defaultSelection == null) {
			getGraphicalViewer().setContents(
					new ProductSystemNode(productSystem, this, null, 0.1));
			return;
		}
		sankeyResult.calculate(defaultSelection);
		double cutoff = sankeyResult.findCutoff(30);
		update(defaultSelection, cutoff);
	}

	public Object getDefaultSelection() {
		if (result == null)
			return null;
		if (result.hasImpactResults()) {
			Set<ImpactCategoryDescriptor> categories = result
					.getImpactDescriptors();
			if (!categories.isEmpty())
				return categories.iterator().next();
		}
		Set<FlowDescriptor> flows = result.getFlowDescriptors();
		if (!flows.isEmpty())
			return flows.iterator().next();
		return null;
	}

	@Override
	public void dispose() {
		if (systemNode != null)
			systemNode.dispose();
		result = null;
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	public ProductSystemNode getModel() {
		return systemNode;
	}

	public double getProductSystemResult() {
		return sankeyResult.getTotalResult(productSystem.getReferenceProcess()
				.getId());
	}

	public double getZoom() {
		return zoom;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("firstTimeInitialized")) {
			createdLinks.clear();
			updateConnections();
		}
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	public void update(Object selection, double cutoff) {
		if (selection == null || cutoff < 0d || cutoff > 1d)
			return;
		App.run("Calculate sankey results",
				() -> sankeyResult.calculate(selection),
				() -> {
					systemNode = new ProductSystemNode(productSystem,
							SankeyDiagram.this, selection, cutoff);
					createdProcesses.clear();
					createdLinks.clear();
					updateModel(cutoff);
					getGraphicalViewer().deselectAll();
					getGraphicalViewer().setContents(systemNode);
				});
	}

}
