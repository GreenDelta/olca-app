package org.openlca.app.analysis.sankey;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
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
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.NormalizationWeightingSet;
import org.openlca.core.model.ProcessLink;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.AnalysisResult;

public class SankeyDiagram extends GraphicalEditor implements
		PropertyChangeListener {

	public static final String ID = "editor.ProductSystemSankeyDiagram";

	private EntityCache cache = Cache.getEntityCache();
	private SankeyResult sankeyResult;
	private Map<Long, ConnectionLink> createdLinks = new HashMap<>();
	private Map<Long, ProcessNode> createdProcesses = new HashMap<>();
	private ProductSystemNode systemNode;
	private ImpactMethodDescriptor method;
	private NormalizationWeightingSet nwSet;
	private ProductSystem productSystem;
	private AnalysisResult result;

	private double zoom = 1;

	public SankeyDiagram(CalculationSetup setUp, AnalysisResult result) {
		setEditDomain(new DefaultEditDomain(this));
		this.result = result;
		productSystem = setUp.getProductSystem();
		sankeyResult = new SankeyResult(productSystem, result);
		method = setUp.getImpactMethod();
		nwSet = setUp.getNwSet();
		if (productSystem != null) {
			setPartName(productSystem.getName());
		}
	}

	public AnalysisResult getResult() {
		return result;
	}

	private void createConnections(long processId) {
		for (ProcessLink processLink : productSystem
				.getIncomingLinks(processId)) {
			ProcessNode sourceNode = createdProcesses.get(processLink
					.getProviderId());
			ProcessNode targetNode = createdProcesses.get(processLink
					.getRecipientId());
			if (sourceNode != null && targetNode != null) {
				if (!createdLinks.containsKey(processLink.getId())) {
					double ratio = sankeyResult
							.getLinkContribution(processLink);
					ConnectionLink link = new ConnectionLink(sourceNode,
							targetNode, processLink, ratio);
					createdLinks.put(processLink.getId(), link);
					createConnections(sourceNode.getProcess().getId());
				}
			}
		}
	}

	private Long getGreatestRecipient(long processId, int position) {
		List<WeightedProcess> recipients = new ArrayList<>();
		for (ProcessLink link : productSystem.getOutgoingLinks(processId)) {
			WeightedProcess wp = new WeightedProcess();
			wp.id = link.getRecipientId();
			wp.weight = Math.abs(sankeyResult.getLinkContribution(link));
			recipients.add(wp);
		}
		Collections.sort(recipients);
		if (recipients.size() <= position - 1)
			return null;
		return recipients.get(position - 1).id;
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
	 * Search a path to the connected graph for the given process id
	 * 
	 * @param processToConnect
	 *            The id of the process to connect to the graph
	 * @param connectedGraph
	 *            The graph to connect to
	 * @param visited
	 *            List of earlier visited nodes (to detect loops and stop
	 *            recursion)
	 * @return A list of process id's representing the path starting with the
	 *         process itself and ending with the node in the graph to connect
	 *         to
	 */
	private List<Long> searchPathFor(final long processToConnect,
			final List<Long> connectedGraph, final List<Long> visited) {
		List<Long> path = null;
		int x = 1;
		while (path == null) {
			// this while iteration is only for loop protection (if the system
			// is build logically, going down the greatest recipient path should
			// always lead to the connected graph, but if the system has
			// internal logical failures the search could lead to a loop which
			// has to be detected and stopped. In this case the second greatest
			// recipient would be the next to look up and so on)

			// get the x-greatest recipient
			final Long greatestRecipient = getGreatestRecipient(
					processToConnect, x);
			if (greatestRecipient == null) {
				break;
			}
			// if the process was already visited don't go further (loop
			// protection)
			if (!visited.contains(greatestRecipient)) {
				if (connectedGraph.contains(greatestRecipient)) {
					// empty list to end while
					path = new ArrayList<>();
				} else {
					// append all visited nodes and the actual one to a new
					// "visited-list" (for loop protection)
					final List<Long> newVisited = new ArrayList<>();
					newVisited.addAll(visited);
					newVisited.add(greatestRecipient);
					// get further path
					final List<Long> nextPath = searchPathFor(
							greatestRecipient, connectedGraph, newVisited);
					if (nextPath != null) {
						// if a path was found, add the path to the current
						path = new ArrayList<>();
						path.add(greatestRecipient);
						path.addAll(nextPath);
					}
				}
			}
			x++;
		}
		return path;
	}

	/**
	 * Checks if each process has a path to the reference process, if not it
	 * searches a way to the reference or another connected node and adds the
	 * missing nodes
	 * 
	 * @param processIds
	 *            The id's of the processes to be drawn
	 * @return A list of additional process nodes to be drawn
	 */
	private List<Long> stockUpGraph(final List<Long> processIds) {
		final List<Long> unconnected = new ArrayList<>();
		final List<Long> connected = new ArrayList<>();

		// at the beginning only the reference process is definitely connected
		// (implicit)
		for (final Long id : processIds) {
			if (!id.equals(productSystem.getReferenceProcess().getId())) {
				unconnected.add(id);
			} else {
				connected.add(id);
			}
		}

		final Queue<Long> toCheck = new LinkedList<>();
		toCheck.add(productSystem.getReferenceProcess().getId());
		while (!toCheck.isEmpty()) {
			// the actual process id
			final Long actual = toCheck.poll();

			// check each provider and add him to the connected list (if the
			// process should be drawn)
			for (final ProcessLink link : productSystem
					.getIncomingLinks(actual)) {
				final Long providerId = link.getProviderId();
				if (processIds.contains(providerId)) {
					if (unconnected.contains(providerId)) {
						unconnected.remove(providerId);
						connected.add(providerId);
						toCheck.add(providerId);
					}
				}
			}
		}

		// for each unconnected process
		final List<Long> additionalNodes = new ArrayList<>();
		for (final Long processId : unconnected) {
			final List<Long> path = searchPathFor(processId, connected,
					new ArrayList<Long>());
			for (final Long id : path) {
				connected.add(id);
				additionalNodes.add(id);
			}
		}
		return additionalNodes;
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
			// collect all process above the cutoff
			List<Long> processesToDraw = sankeyResult
					.getProcesseIdsAboveCutoff(cutoff);

			// if no process is found add at least the reference process
			if (processesToDraw.size() == 0) {
				processesToDraw
						.add(productSystem.getReferenceProcess().getId());
			}

			// stock up the graph
			final List<Long> additionalNodes = stockUpGraph(processesToDraw);
			for (final Long processId : additionalNodes) {
				if (!processesToDraw.contains(processId)) {
					processesToDraw.add(processId);
				}
			}

			// paint processes
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

		MenuManager manager = new MenuManager();
		SankeySelectionAction action = new SankeySelectionAction();
		action.setSankeyDiagram(this);
		manager.add(action);
		getGraphicalViewer().setContextMenu(manager);

		final GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new SankeyEditPartFactory());
		final ScalableRootEditPart rootEditPart = new ScalableRootEditPart();
		viewer.setRootEditPart(rootEditPart);

		final ZoomManager zoomManager = rootEditPart.getZoomManager();

		// append zoom actions to action registry
		getActionRegistry().registerAction(new ZoomInAction(zoomManager));
		getActionRegistry().registerAction(new ZoomOutAction(zoomManager));

		zoomContributions = new ArrayList<>();
		zoomContributions.add(ZoomManager.FIT_ALL);
		zoomContributions.add(ZoomManager.FIT_HEIGHT);
		zoomContributions.add(ZoomManager.FIT_WIDTH);
		zoomManager.setZoomLevelContributions(zoomContributions);

		// create key handler
		final KeyHandler keyHandler = new KeyHandler();
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
		Object defaultSelection = getDefaultSelection(result);
		if (defaultSelection == null) {
			getGraphicalViewer().setContents(
					new ProductSystemNode(productSystem, this, null, 0.1));
			return;
		}
		sankeyResult.calculate(defaultSelection);
		double cutoff = sankeyResult.findCutoff(30);
		update(defaultSelection, cutoff);
	}

	private Object getDefaultSelection(AnalysisResult result) {
		if (result == null)
			return null;
		if (result.hasImpactResults()) {
			Set<ImpactCategoryDescriptor> categories = result
					.getImpactResults().getImpacts(cache);
			if (!categories.isEmpty())
				return categories.iterator().next();
		}
		Set<FlowDescriptor> flows = result.getFlowResults().getFlows(cache);
		if (!flows.isEmpty())
			return flows.iterator().next();
		return null;
	}

	@Override
	public void dispose() {
		if (systemNode != null)
			systemNode.dispose();
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	public ProductSystemNode getModel() {
		return systemNode;
	}

	public NormalizationWeightingSet getNwSet() {
		return nwSet;
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

	public void update(final Object selection, final double cutoff) {
		if (selection == null || cutoff < 0d || cutoff > 1d)
			return;
		App.run("Calculate sankey results", new Runnable() {
			@Override
			public void run() {
				sankeyResult.calculate(selection);
			}
		}, new Runnable() {
			@Override
			public void run() {
				systemNode = new ProductSystemNode(productSystem,
						SankeyDiagram.this, selection, cutoff);
				createdProcesses.clear();
				createdLinks.clear();
				updateModel(cutoff);
				getGraphicalViewer().deselectAll();
				getGraphicalViewer().setContents(systemNode);
			}
		});
	}

	/**
	 * Combines the id of a process and its weight
	 * 
	 * @author Sebastian Greve
	 * 
	 */
	private class WeightedProcess implements Comparable<WeightedProcess> {

		/**
		 * The id of the process
		 */
		private long id;

		/**
		 * The weight of the process
		 */
		private double weight;

		@Override
		public int compareTo(final WeightedProcess o) {
			return -Double.compare(weight, o.weight);
		}

	}

}
