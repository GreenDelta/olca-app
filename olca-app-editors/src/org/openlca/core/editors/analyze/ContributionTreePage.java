package org.openlca.core.editors.analyze;

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
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.openlca.core.application.Numbers;
import org.openlca.core.database.IDatabase;
import org.openlca.core.editors.ContributionImage;
import org.openlca.core.editors.FlowImpactSelection;
import org.openlca.core.editors.FlowImpactSelection.EventHandler;
import org.openlca.core.editors.model.FlowInfo;
import org.openlca.core.model.Flow;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;
import org.openlca.core.model.results.AnalysisResult;
import org.openlca.core.model.results.ContributionTree;
import org.openlca.core.model.results.ContributionTreeCalculator;
import org.openlca.core.model.results.ContributionTreeNode;
import org.openlca.core.model.results.LinkContributions;
import org.openlca.ui.UI;

public class ContributionTreePage extends FormPage {

	private AnalyzeEditor editor;
	private AnalysisResult result;
	private IDatabase database;
	private TreeViewer contributionTree;
	private ContributionTreeCalculator calculator;
	private Object selection;

	private static final String[] HEADERS = { "Contribution", "Process",
			"Amount", "Unit" };

	public ContributionTreePage(AnalyzeEditor editor, AnalysisResult result) {
		super(editor, "analysis.ContributionTreePage", "Contribution tree");
		this.editor = editor;
		this.result = result;
		this.database = editor.getDatabase();
		LinkContributions linkContributions = LinkContributions.calculate(
				result.getSetup().getProductSystem(), result.getProductIndex(),
				result.getScalingFactors());
		this.calculator = new ContributionTreeCalculator(result,
				linkContributions);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		FormToolkit toolkit = managedForm.getToolkit();

		ScrolledForm form = UI.formHeader(managedForm, "Contribution tree");
		Composite body = UI.formBody(form, toolkit);

		Composite composite = toolkit.createComposite(body);
		UI.gridLayout(composite, 2);
		FlowImpactSelection.onDatabase(database).withAnalysisResult(result)
				.withEventHandler(new SelectionHandler())
				.create(composite, toolkit);

		Composite treeContainer = toolkit.createComposite(body);
		UI.gridLayout(treeContainer, 1);
		UI.gridData(treeContainer, true, true);

		contributionTree = new TreeViewer(treeContainer);
		contributionTree.setAutoExpandLevel(2);
		contributionTree.getTree().setLinesVisible(false);
		contributionTree.getTree().setHeaderVisible(true);

		for (int i = 0; i < HEADERS.length; i++) {
			final TreeColumn c = new TreeColumn(contributionTree.getTree(),
					SWT.NULL);
			c.setText(HEADERS[i]);
		}
		contributionTree.setColumnProperties(HEADERS);
		contributionTree.setContentProvider(new ContributionContentProvider());
		contributionTree.setLabelProvider(new ContributionLabelProvider());
		contributionTree.setSorter(new ContributionSorter());
		UI.gridData(contributionTree.getTree(), true, true);
		toolkit.adapt(contributionTree.getTree(), false, false);
		toolkit.paintBordersFor(contributionTree.getTree());

		form.reflow(true);
		selection = result.getFlows()[0];
		contributionTree.setInput(calculator.calculate(result.getFlows()[0]));
		for (TreeColumn column : contributionTree.getTree().getColumns())
			column.pack();
	}

	private class SelectionHandler implements EventHandler {

		@Override
		public void flowSelected(Flow flow) {
			selection = flow;
			contributionTree.setInput(calculator.calculate(flow));
		}

		@Override
		public void impactCategorySelected(
				ImpactCategoryDescriptor impactCategory) {
			selection = impactCategory;
			contributionTree.setInput(calculator.calculate(impactCategory));
		}

	}

	private class ContributionContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!(parentElement instanceof ContributionTreeNode))
				return null;
			return ((ContributionTreeNode) parentElement).getChildren()
					.toArray();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (!(inputElement instanceof ContributionTree))
				return null;
			return new Object[] { ((ContributionTree) inputElement).getRoot() };
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!(element instanceof ContributionTreeNode))
				return false;
			return !((ContributionTreeNode) element).getChildren().isEmpty();
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
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
		public Image getColumnImage(Object element, int columnIndex) {
			if (!(element instanceof ContributionTreeNode) || element == null)
				return null;
			if (columnIndex != 1)
				return null;

			ContributionTreeNode node = (ContributionTreeNode) element;
			return image.getForTable(getContribution(node));
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof ContributionTreeNode))
				return null;
			ContributionTreeNode node = (ContributionTreeNode) element;
			switch (columnIndex) {
			case 0:
				return Numbers.percent(getContribution(node));
			case 1:
				return node.getProcess().getName();
			case 2:
				return Numbers.format(getSingleAmount(node));
			case 3:
				return getUnit();
			default:
				return null;
			}
		}

		private String getUnit() {
			if (selection instanceof Flow) {
				FlowInfo info = editor.getFlowInfo((Flow) selection);
				return info.getUnit();
			} else if (selection instanceof ImpactCategoryDescriptor)
				return ((ImpactCategoryDescriptor) selection)
						.getReferenceUnit();
			return null;
		}

		private double getTotalAmount() {
			return ((ContributionTree) contributionTree.getInput()).getRoot()
					.getAmount();
		}

		private double getContribution(ContributionTreeNode node) {
			double singleResult = getSingleAmount(node);
			if (singleResult == 0)
				return 0;
			double referenceResult = getTotalAmount();
			if (referenceResult == 0)
				return 0;
			double contribution = singleResult / referenceResult;
			if (contribution > 1)
				return 1;
			return contribution;
		}

		private double getSingleAmount(ContributionTreeNode node) {
			return node.getAmount();
		}

	}

	private class ContributionSorter extends ViewerSorter {

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (!(e1 instanceof ContributionTreeNode && e2 instanceof ContributionTreeNode)
					|| e1 == null || e2 == null)
				return 0;
			ContributionTreeNode node1 = (ContributionTreeNode) e1;
			ContributionTreeNode node2 = (ContributionTreeNode) e2;
			return -1 * Double.compare(node1.getAmount(), node2.getAmount());
		}
	}

}
