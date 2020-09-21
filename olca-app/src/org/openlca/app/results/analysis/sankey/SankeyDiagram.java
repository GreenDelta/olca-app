package org.openlca.app.results.analysis.sankey;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.openlca.app.App;
import org.openlca.app.results.analysis.sankey.actions.SankeyMenu;
import org.openlca.app.results.analysis.sankey.model.Link;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.ProcessProduct;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.Sankey;
import org.openlca.util.Strings;

public class SankeyDiagram extends GraphicalEditor implements PropertyChangeListener {

	public static final String ID = "editor.ProductSystemSankeyDiagram";
	public final DQResult dqResult;
	public final FullResult result;
	public ProductSystemNode node;
	public double zoom = 1;
	private boolean routed = false;
	public Sankey<?> sankey;
	public final List<Link> createdLinks = new ArrayList<>();
	public final Map<ProcessProduct, ProcessNode> createdNodes = new HashMap<>();
	private final ProductSystem productSystem;

	public SankeyDiagram(FullResult result, DQResult dqResult, CalculationSetup setup) {
		this.dqResult = dqResult;
		setEditDomain(new DefaultEditDomain(this));
		this.result = result;
		productSystem = setup.productSystem;
		if (productSystem != null) {
			setPartName(productSystem.name);
		}
	}

	private void createConnectionss() {


		// TODO: remove dead code
		/*
		Set<Long> handled = new HashSet<>();
		Stack<Long> processes = new Stack<>();
		processes.add(startProcessId);
		while (!processes.isEmpty()) {
			long nextId = processes.pop();
			handled.add(nextId);
			for (ProcessLink link : linkSearchMap.getIncomingLinks(nextId)) {
				if (createdLinks.containsKey(link))
					continue;
				ProcessNode source = createdProcesses.get(link.providerId);
				ProcessNode target = createdProcesses.get(link.processId);
				if (source == null || target == null)
					continue;
				double ratio = sankeyResult.getLinkContribution(link);

				if (handled.contains(source.process.id))
					continue;
				processes.add(source.process.id);
			}
		}
		*/
	}

	private void updateConnections() {
		createdLinks.clear();
		if (sankey == null)
			return;
		sankey.traverse(node -> {
			var target = createdNodes.get(node.product);
			if (target ==null)
				return;
			for (var provider : node.providers) {
				var source = createdNodes.get(provider.product);
				if (source == null)
					continue;
				var share = sankey.getLinkShare(provider, node);
				createdLinks.add(new Link(source, target, share));
			}
		});
		for (var link : createdLinks) {
			link.link();
		}
	}

	private void updateModel(Object selection, double cutoff) {
		sankey = Sankey.of(selection, result)
				.withMinimumShare(cutoff)
				.withMaximumNodeCount(500)
				.build();
		sankey.traverse(n -> {
			var node = new ProcessNode(n);
			createdNodes.put(n.product, node);
			this.node.addChild(node);
		});

		// TODO: remove dead code
		/*
		Map<Long, CategorizedDescriptor> processes = new HashMap<>();
		for (CategorizedDescriptor d : result.getProcesses())
			processes.put(d.id, d);
		if (cutoff == 0) {
			for (Long processId : productSystem.processes) {
				CategorizedDescriptor d = processes.get(processId);
				if (d != null) {
					node.addChild(createNode(d));
				}
			}
		} else {
			long refProcess = productSystem.referenceProcess.id;
			Set<Long> processesToDraw = SankeyProcessList.calculate(
					sankeyResult, refProcess, cutoff, linkSearchMap);
			for (final Long processId : processesToDraw) {
				CategorizedDescriptor process = processes.get(processId);
				if (process != null) {
					node.addChild(createNode(process));
				}
			}
		}

		 */
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();

		MenuManager menu = SankeyMenu.create(this);
		getGraphicalViewer().setContextMenu(menu);

		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setEditPartFactory(new SankeyEditPartFactory());
		ScalableRootEditPart root = new ScalableRootEditPart();
		viewer.setRootEditPart(root);

		// append zoom actions to action registry
		ZoomManager zoom = root.getZoomManager();
		getActionRegistry().registerAction(new ZoomInAction(zoom));
		getActionRegistry().registerAction(new ZoomOutAction(zoom));
		zoom.setZoomLevelContributions(Arrays.asList(
				ZoomManager.FIT_ALL,
				ZoomManager.FIT_HEIGHT,
				ZoomManager.FIT_WIDTH));

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
		var editPart = new ScalableRootEditPart() {

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
		};

		getGraphicalViewer().setRootEditPart(editPart);

		// zoom listener
		editPart.getZoomManager()
				.addZoomListener(z -> zoom = z);
		editPart.getZoomManager().setZoomLevels(new double[]{
				0.005, 0.01, 0.02, 0.0375, 0.075,
				0.125, 0.25, 0.5, 0.75, 1.0, 1.5,
				2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0,
				40.0, 80.0, 150.0, 300.0, 500.0, 1000.0
		});

		getGraphicalViewer()
				.getEditDomain()
				.setActiveTool(new PanningSelectionTool());
		initContent();
	}

	private void initContent() {
		Object s = getDefaultSelection();
		if (s == null) {
			getGraphicalViewer().setContents(
					new ProductSystemNode(productSystem, this, null, 0.1));
			return;
		}
		update(s, 0.01); // => TODO: also calls sankeyResult.calculate(s)
	}

	public Object getDefaultSelection() {
		if (result == null)
			return null;

		if (result.hasImpactResults()) {
			var impact = result.getImpacts()
					.stream()
					.min((i1, i2) -> Strings.compare(i1.name, i2.name))
					.orElse(null);
			if (impact != null)
				return impact;
		}

		return result.getFlows().stream().min((f1, f2) -> {
			if (f1.flow == null || f2.flow == null)
				return 0;
			return Strings.compare(f1.flow.name, f2.flow.name);
		}).orElse(null);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	public double getProductSystemResult() {
		// TODO: check
		return sankey == null || sankey.root == null
				? 0
				: sankey.root.total;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
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
				() -> sankey = Sankey.of(selection, result)
						.withMinimumShare(cutoff)
						.withMaximumNodeCount(500)
						.build(),
				() -> {
					node = new ProductSystemNode(
							productSystem, this, selection, cutoff);
					createdLinks.clear();
					updateModel(selection, cutoff);
					getGraphicalViewer().deselectAll();
					getGraphicalViewer().setContents(node);
					node.setRouted(routed);
				});
	}

	public boolean isRouted() {
		return routed;
	}

	public void switchRouting() {
		routed = !routed;
		if (node != null) {
			node.setRouted(routed);
		}
	}

}
