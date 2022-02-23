package org.openlca.app.results.analysis.sankey;

import java.util.Arrays;
import java.util.HashMap;
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
import org.eclipse.swt.SWT;
import org.openlca.app.App;
import org.openlca.app.results.ResultEditor;
import org.openlca.app.results.analysis.sankey.actions.SankeyMenu;
import org.openlca.app.results.analysis.sankey.model.Link;
import org.openlca.app.results.analysis.sankey.model.ProcessNode;
import org.openlca.app.results.analysis.sankey.model.ProductSystemNode;
import org.openlca.app.results.analysis.sankey.model.SankeyEditPartFactory;
import org.openlca.core.math.data_quality.DQResult;
import org.openlca.core.matrix.index.TechFlow;
import org.openlca.core.model.RootEntity;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.ResultItemView;
import org.openlca.core.results.Sankey;
import org.openlca.util.Strings;

public class SankeyDiagram extends GraphicalEditor {

	public static final String ID = "editor.ProductSystemSankeyDiagram";
	public final DQResult dqResult;
	public final FullResult result;
	public final ResultItemView resultItems;

	public Sankey<?> sankey;
	public ProductSystemNode node;
	public double zoom = 1;
	public double cutoff = 0.0;
	public int maxCount = 25;
	public Object selection;
	private boolean routed = true;

	public final Map<TechFlow, ProcessNode> createdNodes = new HashMap<>();
	private final RootEntity calculationTarget;

	public SankeyDiagram(ResultEditor<FullResult> parent) {
		this.dqResult = parent.dqResult;
		this.result = parent.result;
		this.resultItems = parent.resultItems;
		calculationTarget = parent.setup.target();
		setEditDomain(new DefaultEditDomain(this));
		if (calculationTarget != null) {
			setPartName(calculationTarget.name);
		}
	}

	@Override
	protected void configureGraphicalViewer() {
		super.configureGraphicalViewer();

		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setContextMenu(SankeyMenu.create(this));
		viewer.setEditPartFactory(new SankeyEditPartFactory());
		var root = new ScalableRootEditPart();
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
		var keyHandler = new KeyHandler();
		keyHandler.put(KeyStroke.getPressed('+', SWT.KEYPAD_ADD, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_IN));
		keyHandler.put(KeyStroke.getPressed('-', SWT.KEYPAD_SUBTRACT, 0),
				getActionRegistry().getAction(GEFActionConstants.ZOOM_OUT));
		viewer.setKeyHandler(keyHandler);

		viewer.setProperty(
				MouseWheelHandler.KeyGenerator.getKey(SWT.NONE),
				MouseWheelZoomHandler.SINGLETON);
	}

	@Override
	protected void initializeGraphicalViewer() {

		var editPart = new ScalableRootEditPart() {

			@Override
			protected LayeredPane createPrintableLayers() {
				var pane = new LayeredPane();
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
		editPart.getZoomManager().setZoomLevels(new double[] {
				0.005, 0.01, 0.02, 0.0375, 0.075,
				0.125, 0.25, 0.5, 0.75, 1.0, 1.5,
				2.0, 2.5, 3.0, 4.0, 5.0, 10.0, 20.0,
				40.0, 80.0, 150.0, 300.0, 500.0, 1000.0
		});

		getGraphicalViewer()
				.getEditDomain()
				.setActiveTool(new PanningSelectionTool());

		getEditorSite();
	}

	public void initContent() {
		if (result == null)
			return;
		Object initial = null;
		if (result.hasImpacts()) {
			initial = result.getImpacts()
					.stream()
					.min((i1, i2) -> Strings.compare(i1.name, i2.name))
					.orElse(null);
		}
		if (initial == null) {
			initial = result.getFlows()
					.stream()
					.min((f1, f2) -> {
						if (f1.flow() == null || f2.flow() == null)
							return 0;
						return Strings.compare(f1.flow().name, f2.flow().name);
					})
					.orElse(null);
		}
		// TODO costs...
		if (initial == null)
			return;
		update(initial, cutoff, maxCount);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public GraphicalViewer getGraphicalViewer() {
		return super.getGraphicalViewer();
	}

	public void update(Object selection, double cutoff, int maxCount) {
		if (selection == null
				|| cutoff < 0d
				|| cutoff > 1d
				|| maxCount < 0)
			return;
		this.selection = selection;
		this.cutoff = cutoff;
		this.maxCount = maxCount;

		App.runWithProgress("Calculate sankey results",
				() -> sankey = Sankey.of(selection, result)
						.withMinimumShare(cutoff)
						.withMaximumNodeCount(maxCount)
						.build(),
				() -> {

					node = new ProductSystemNode(calculationTarget, this);
					updateModel();
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

	private void updateModel() {
		if (sankey == null)
			return;

		// create the nodes
		sankey.traverse(n -> {
			var node = new ProcessNode(this.node, n);
			createdNodes.put(n.product, node);
			this.node.processNodes.add(node);
		});

		// create the links
		sankey.traverse(node -> {
			var target = createdNodes.get(node.product);
			if (target == null)
				return;
			for (var provider : node.providers) {
				var source = createdNodes.get(provider.product);
				if (source == null)
					continue;
				var linkShare = sankey.getLinkShare(provider, node);
				var share = linkShare * provider.share;
				var link = new Link(source, target, share);
				link.link();
			}
		});
	}
}
