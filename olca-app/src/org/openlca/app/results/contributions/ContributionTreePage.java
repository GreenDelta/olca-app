package org.openlca.app.results.contributions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.M;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.ResultTypeSelection;
import org.openlca.app.components.ResultTypeSelection.EventHandler;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.results.FullResult;
import org.openlca.core.results.UpstreamNode;
import org.openlca.core.results.UpstreamTree;

public class ContributionTreePage extends FormPage {

	private FullResult result;
	private TreeViewer tree;
	private Object selection;
	private CalculationSetup setup;

	private static final String[] HEADERS = { M.Contribution,
			M.Process, M.Amount, M.Unit };

	public ContributionTreePage(FormEditor editor, FullResult result, CalculationSetup setup) {
		super(editor, "analysis.ContributionTreePage", M.ContributionTree);
		this.result = result;
		this.setup = setup;
		Iterator<FlowDescriptor> it = result.getFlows().iterator();
		if (it.hasNext())
			selection = it.next();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit tk = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
				Labels.getDisplayName(setup.productSystem),
				Images.get(result));
		Composite body = UI.formBody(form, tk);
		Composite comp = tk.createComposite(body);
		UI.gridLayout(comp, 2);
		ResultTypeSelection selector = ResultTypeSelection
				.on(result)
				.withEventHandler(new SelectionHandler())
				.create(comp, tk);
		Composite treeComp = tk.createComposite(body);
		UI.gridLayout(treeComp, 1);
		UI.gridData(treeComp, true, true);
		createTree(tk, treeComp);
		form.reflow(true);
		selector.selectWithEvent(selection);
	}

	private void createTree(FormToolkit tk, Composite comp) {
		tree = Trees.createViewer(comp, HEADERS,
				new ContributionLabelProvider());
		tree.setAutoExpandLevel(2);
		tree.getTree().setLinesVisible(false);
		tree.setContentProvider(new ContributionContentProvider());
		tk.adapt(tree.getTree(), false, false);
		tk.paintBordersFor(tree.getTree());
		Action onOpen = Actions.onOpen(() -> {
			UpstreamNode n = Viewers.getFirstSelected(tree);
			if (n == null || n.provider == null)
				return;
			App.openEditor(n.provider.process);
		});
		Actions.bind(tree, onOpen, TreeClipboard.onCopy(tree));
		Trees.onDoubleClick(tree, e -> onOpen.run());
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
		Trees.bindColumnWidths(tree.getTree(),
				0.20, 0.50, 0.20, 0.10);
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(FlowDescriptor flow) {
			selection = flow;
			UpstreamTree model = result.getTree(flow);
			tree.setInput(model);
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impact) {
			selection = impact;
			UpstreamTree model = result.getTree(impact);
			tree.setInput(model);
		}

		@Override
		public void costResultSelected(CostResultDescriptor cost) {
			selection = cost;
			UpstreamTree model = cost.forAddedValue
					? result.getAddedValueTree()
					: result.getCostTree();
			tree.setInput(model);
		}
	}

	private class ContributionContentProvider implements ITreeContentProvider {

		private UpstreamTree tree;

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof UpstreamNode))
				return null;
			if (tree == null)
				return null;
			UpstreamNode node = (UpstreamNode) parent;
			return tree.childs(node).toArray();
		}

		@Override
		public Object[] getElements(Object input) {
			if (!(input instanceof UpstreamTree))
				return null;
			return new Object[] { ((UpstreamTree) input).root };
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object elem) {
			if (!(elem instanceof UpstreamNode))
				return false;
			UpstreamNode node = (UpstreamNode) elem;
			return !tree.childs(node).isEmpty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object old, Object input) {
			if (!(input instanceof UpstreamTree)) {
				tree = null;
				return;
			}
			tree = (UpstreamTree) input;
		}

		@Override
		public void dispose() {
		}

	}

	private class ContributionLabelProvider extends BaseLabelProvider implements
			ITableLabelProvider {

		private ContributionImage image = new ContributionImage(
				Display.getCurrent());

		@Override
		public void dispose() {
			image.dispose();
			super.dispose();
		}

		@Override
		public Image getColumnImage(Object obj, int col) {
			if (!(obj instanceof UpstreamNode))
				return null;
			UpstreamNode n = (UpstreamNode) obj;
			if (col == 1 && n.provider != null) {
				return Images.get(n.provider.process);
			}
			if (col == 2) {
				return image.getForTable(getContribution(n));
			}
			return null;
		}

		@Override
		public String getColumnText(Object obj, int col) {
			if (!(obj instanceof UpstreamNode))
				return null;
			UpstreamNode node = (UpstreamNode) obj;
			switch (col) {
			case 0:
				return Numbers.percent(getContribution(node));
			case 1:
				return Labels.getDisplayName(node.provider.process);
			case 2:
				return Numbers.format(node.result);
			case 3:
				return getUnit();
			default:
				return null;
			}
		}

		private String getUnit() {
			if (selection instanceof FlowDescriptor) {
				FlowDescriptor flow = (FlowDescriptor) selection;
				return Labels.getRefUnit(flow);
			} else if (selection instanceof ImpactCategoryDescriptor) {
				ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) selection;
				return impact.referenceUnit;
			} else if (selection instanceof CostResultDescriptor) {
				return Labels.getReferenceCurrencyCode();
			}
			return null;
		}

		private double getContribution(UpstreamNode node) {
			if (node.result == 0)
				return 0;
			double total = ((UpstreamTree) tree.getInput()).root.result;
			if (total == 0)
				return 0;
			return node.result / total;
		}

	}

}
