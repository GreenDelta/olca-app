package org.openlca.app.results.analysis;

import java.util.Iterator;

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
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.app.App;
import org.openlca.app.Messages;
import org.openlca.app.components.ContributionImage;
import org.openlca.app.components.FlowImpactSelection;
import org.openlca.app.components.FlowImpactSelection.EventHandler;
import org.openlca.app.db.Cache;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.Numbers;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.core.database.EntityCache;
import org.openlca.core.matrix.LongPair;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.FullResultProvider;
import org.openlca.core.results.UpstreamTree;
import org.openlca.core.results.UpstreamTreeNode;

public class ContributionTreePage extends FormPage {

	private EntityCache cache = Cache.getEntityCache();
	private FullResultProvider result;
	private TreeViewer tree;
	private Object selection;

	private static final String[] HEADERS = { Messages.Contribution,
			Messages.Process, Messages.Amount, Messages.Unit };

	public ContributionTreePage(AnalyzeEditor editor, FullResultProvider result) {
		super(editor, "analysis.ContributionTreePage",
				Messages.ContributionTree);
		this.result = result;
		Iterator<FlowDescriptor> it = result.getFlowDescriptors().iterator();
		if (it.hasNext())
			selection = it.next();
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();
		ScrolledForm form = UI.formHeader(managedForm,
				Messages.ContributionTree);
		Composite body = UI.formBody(form, toolkit);
		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		FlowImpactSelection selector = FlowImpactSelection
				.on(result, Cache.getEntityCache())
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);
		Composite treeContainer = toolkit.createComposite(body);
		UI.gridLayout(treeContainer, 1);
		UI.gridData(treeContainer, true, true);
		createTree(toolkit, treeContainer);
		form.reflow(true);
		for (TreeColumn column : tree.getTree().getColumns())
			column.pack();
		selector.selectWithEvent(selection);
	}

	private void createTree(FormToolkit toolkit, Composite treeContainer) {
		tree = new TreeViewer(treeContainer, SWT.FULL_SELECTION
				| SWT.MULTI | SWT.BORDER);
		tree.setAutoExpandLevel(2);
		tree.getTree().setLinesVisible(false);
		tree.getTree().setHeaderVisible(true);
		for (int i = 0; i < HEADERS.length; i++) {
			TreeColumn c = new TreeColumn(tree.getTree(), SWT.NULL);
			c.setText(HEADERS[i]);
		}
		tree.setColumnProperties(HEADERS);
		tree.setContentProvider(new ContributionContentProvider());
		tree.setLabelProvider(new ContributionLabelProvider());
		tree.setSorter(new ContributionSorter());
		UI.gridData(tree.getTree(), true, true);
		toolkit.adapt(tree.getTree(), false, false);
		toolkit.paintBordersFor(tree.getTree());
		createMenu();
	}

	private void createMenu() {
		MenuManager manager = new MenuManager();
		manager.add(new OpenEditorAction());
		manager.add(TreeClipboard.onCopy(tree));
		Menu menu = manager.createContextMenu(tree.getControl());
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
				BaseDescriptor d = cache
						.get(ProcessDescriptor.class, processId);
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
			} else if (selection instanceof ImpactCategoryDescriptor)
				return ((ImpactCategoryDescriptor) selection)
						.getReferenceUnit();
			return null;
		}

		private double getTotalAmount() {
			return ((UpstreamTree) tree.getInput()).getRoot()
					.getAmount();
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
			if (!(e1 instanceof UpstreamTreeNode && e2 instanceof UpstreamTreeNode)
					|| e1 == null || e2 == null)
				return 0;
			UpstreamTreeNode node1 = (UpstreamTreeNode) e1;
			UpstreamTreeNode node2 = (UpstreamTreeNode) e2;
			return -1 * Double.compare(node1.getAmount(), node2.getAmount());
		}
	}

	private class OpenEditorAction extends Action {

		public OpenEditorAction() {
			setText(Messages.Open);
			setImageDescriptor(ImageType.PROCESS_ICON.getDescriptor());
		}

		@Override
		public void run() {
			Object selection = Viewers.getFirstSelected(tree);
			if (!(selection instanceof UpstreamTreeNode))
				return;
			UpstreamTreeNode node = (UpstreamTreeNode) selection;
			LongPair processProduct = node.getProcessProduct();
			ProcessDescriptor process = Cache.getEntityCache().get(
					ProcessDescriptor.class, processProduct.getFirst());
			if (process != null)
				App.openEditor(process);
		}
	}
}
