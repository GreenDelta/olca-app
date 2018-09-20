package org.openlca.app.results.contributions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
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
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Actions;
import org.openlca.app.util.CostResultDescriptor;
import org.openlca.app.util.CostResults;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.trees.Trees;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.core.database.EntityCache;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResultProvider;
import org.openlca.core.results.UpstreamTree;
import org.openlca.core.results.UpstreamTreeNode;

public class ContributionTreePage extends FormPage {

	private EntityCache cache = Cache.getEntityCache();
	private Map<Long, ProcessDescriptor> processDescriptors = new HashMap<>();
	private FullResultProvider result;
	private TreeViewer tree;
	private Object selection;
	private CalculationSetup setup;

	private static final String[] HEADERS = { M.Contribution,
			M.Process, M.Amount, M.Unit };

	public ContributionTreePage(FormEditor editor, FullResultProvider result, CalculationSetup setup) {
		super(editor, "analysis.ContributionTreePage", M.ContributionTree);
		this.result = result;
		this.setup = setup;
		for (ProcessDescriptor desc : result.getProcessDescriptors())
			processDescriptors.put(desc.getId(), desc);
		Iterator<FlowDescriptor> it = result.getFlowDescriptors().iterator();
		if (it.hasNext())
			selection = it.next();
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		FormToolkit toolkit = mform.getToolkit();
		ScrolledForm form = UI.formHeader(mform,
				Labels.getDisplayName(setup.productSystem),
				Images.get(result));
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		ResultTypeSelection selector = ResultTypeSelection
				.on(result, Cache.getEntityCache())
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		Composite treeContainer = toolkit.createComposite(body);
		UI.gridLayout(treeContainer, 1);
		UI.gridData(treeContainer, true, true);
		createTree(toolkit, treeContainer);
		form.reflow(true);
		selector.selectWithEvent(selection);
	}

	private void createTree(FormToolkit toolkit, Composite treeContainer) {
		tree = Trees.createViewer(treeContainer, HEADERS, new ContributionLabelProvider());
		tree.setAutoExpandLevel(2);
		tree.getTree().setLinesVisible(false);
		tree.setContentProvider(new ContributionContentProvider());
		tree.setSorter(new ContributionSorter());
		toolkit.adapt(tree.getTree(), false, false);
		toolkit.paintBordersFor(tree.getTree());
		createMenu();
		Trees.bindColumnWidths(tree.getTree(), 0.20, 0.50, 0.20, 0.10);
		tree.getTree().getColumns()[2].setAlignment(SWT.RIGHT);
	}

	private void createMenu() {
		MenuManager mm = new MenuManager();
		mm.add(new OpenEditorAction());
		mm.add(TreeClipboard.onCopy(tree));
		mm.add(Actions.create(M.ExpandAll,
				Icon.EXPAND.descriptor(), () -> {
					tree.expandAll();
				}));
		Menu menu = mm.createContextMenu(tree.getControl());
		tree.getControl().setMenu(menu);
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
				ImpactCategoryDescriptor impactCategory) {
			selection = impactCategory;
			UpstreamTree model = result.getTree(impactCategory);
			tree.setInput(model);
		}

		@Override
		public void costResultSelected(CostResultDescriptor cost) {
			selection = cost;
			UpstreamTree model = result.getCostTree();
			if (cost.forAddedValue)
				CostResults.forAddedValues(model);
			tree.setInput(model);
		}
	}

	private class ContributionContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parent) {
			if (!(parent instanceof UpstreamTreeNode))
				return null;
			UpstreamTreeNode node = (UpstreamTreeNode) parent;
			return node.getChildren().toArray();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (!(inputElement instanceof UpstreamTree))
				return null;
			return new Object[] { ((UpstreamTree) inputElement).getRoot() };
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof UpstreamTreeNode))
				return false;
			UpstreamTreeNode node = (UpstreamTreeNode) element;
			return !node.getChildren().isEmpty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object old, Object newInput) {
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
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof UpstreamTreeNode) || element == null)
				return null;
			if (columnIndex != 1)
				return null;
			UpstreamTreeNode node = (UpstreamTreeNode) element;
			return image.getForTable(getContribution(node));
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof UpstreamTreeNode))
				return null;
			UpstreamTreeNode node = (UpstreamTreeNode) element;
			switch (columnIndex) {
			case 0:
				return Numbers.percent(getContribution(node));
			case 1:
				long processId = node.getProcessProduct().getFirst();
				BaseDescriptor d = processDescriptors.get(processId);
				return Labels.getDisplayName(d);
			case 2:
				return Numbers.format(getSingleAmount(node));
			case 3:
				return getUnit();
			default:
				return null;
			}
		}

		private String getUnit() {
			if (selection instanceof FlowDescriptor) {
				FlowDescriptor flow = (FlowDescriptor) selection;
				return Labels.getRefUnit(flow, cache);
			} else if (selection instanceof ImpactCategoryDescriptor) {
				ImpactCategoryDescriptor impact = (ImpactCategoryDescriptor) selection;
				return impact.getReferenceUnit();
			} else if (selection instanceof CostResultDescriptor) {
				return Labels.getReferenceCurrencyCode();
			}
			return null;
		}

		private double getTotalAmount() {
			return ((UpstreamTree) tree.getInput()).getRoot().getAmount();
		}

		private double getContribution(UpstreamTreeNode node) {
			double singleResult = getSingleAmount(node);
			if (singleResult == 0)
				return 0;
			double referenceResult = Math.abs(getTotalAmount());
			if (referenceResult == 0)
				return 0;
			double contribution = singleResult / referenceResult;
			return contribution;
		}

		private double getSingleAmount(UpstreamTreeNode node) {
			return node.getAmount();
		}

	}

	private class ContributionSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof UpstreamTreeNode
					&& e2 instanceof UpstreamTreeNode)
					|| e1 == null || e2 == null)
				return 0;
			UpstreamTreeNode node1 = (UpstreamTreeNode) e1;
			UpstreamTreeNode node2 = (UpstreamTreeNode) e2;
			return -1 * Double.compare(node1.getAmount(), node2.getAmount());
		}
	}

	private class OpenEditorAction extends Action {

		public OpenEditorAction() {
			setText(M.Open);
			setImageDescriptor(Images.descriptor(ModelType.PROCESS));
		}

		@Override
		public void run() {
			Object selection = Viewers.getFirstSelected(tree);
			if (!(selection instanceof UpstreamTreeNode))
				return;
			UpstreamTreeNode node = (UpstreamTreeNode) selection;
			LongPair processProduct = node.getProcessProduct();
			ProcessDescriptor process = processDescriptors.get(processProduct.getFirst());
			if (process != null)
				App.openEditor(process);
		}
	}
}
