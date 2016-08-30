package org.openlca.app.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.navigation.filters.FlowTypeFilter;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.viewers.Viewers;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowDescriptor;

public class ProcessWizard extends AbstractWizard<Process> {

	private Flow refFlow;

	/**
	 * Optionally set the reference flow of process. This function is used when
	 * a process is directly created from a product flow.
	 */
	public void setRefFlow(Flow refFlow) {
		this.refFlow = refFlow;
	}

	@Override
	protected String getTitle() {
		return M.NewProcess;
	}

	@Override
	protected AbstractWizardPage<Process> createPage() {
		return new Page();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.PROCESS;
	}

	private class Page extends AbstractWizardPage<Process> {

		private Composite contentStack;
		private Button createRefFlowCheck;
		private Composite labelStack;
		private TreeViewer productTree;
		private FlowPropertyViewer flowPropertyViewer;
		private Label selectProductLabel;
		private Label selectFlowPropertyLabel;
		private Composite flowPropertyContainer;
		private Composite productTreeContainer;
		private Text filterText;

		protected Page() {
			super("ProcessWizardPage");
			setTitle(M.NewProcess);
			setMessage(M.NewProcess);
			setPageComplete(false);
		}

		@Override
		protected void checkInput() {
			super.checkInput();
			boolean createFlow = createRefFlowCheck.getSelection();
			String err = M.NoQuantitativeReferenceSelected;
			if (createFlow) {
				if (flowPropertyViewer.getSelected() == null)
					setErrorMessage(err);
			} else {
				Flow flow = getSelectedFlow();
				if (flow == null)
					setErrorMessage(err);
			}
			setPageComplete(getErrorMessage() == null);
		}

		@Override
		protected void createContents(Composite comp) {
			new Label(comp, SWT.NONE);
			createRefFlowCheck(comp);
			filterText = UI.formText(comp, M.QuantitativeReference);
			createLabelStack(comp);
			contentStack = new Composite(comp, SWT.NONE);
			UI.gridData(contentStack, true, true).heightHint = 200;
			contentStack.setLayout(new StackLayout());
			createProductViewer();
			createPropertyViewer();
			((StackLayout) labelStack.getLayout()).topControl = selectProductLabel;
			((StackLayout) contentStack.getLayout()).topControl = productTreeContainer;
			labelStack.layout();
			contentStack.layout();
			if (refFlow != null) {
				FlowDescriptor d = Descriptors.toDescriptor(refFlow);
				INavigationElement<?> e = Navigator.findElement(d);
				ISelection s = new StructuredSelection(e);
				productTree.setSelection(s, true);
				String name = refFlow.getName() != null ? refFlow.getName() : "";
				nameText.setText(name);
				checkInput();
			}
		}

		private void createRefFlowCheck(Composite container) {
			createRefFlowCheck = new Button(container, SWT.CHECK);
			createRefFlowCheck.setText(M.CreateANewProductFlowForTheProcess);
			Controls.onSelect(createRefFlowCheck, e -> {
				boolean createFlow = createRefFlowCheck.getSelection();
				StackLayout labelLayout = (StackLayout) labelStack.getLayout();
				StackLayout contentLayout = (StackLayout) contentStack
						.getLayout();
				if (createFlow) {
					labelLayout.topControl = selectFlowPropertyLabel;
					contentLayout.topControl = flowPropertyContainer;
					filterText.setEnabled(false);
				} else {
					labelLayout.topControl = selectProductLabel;
					contentLayout.topControl = productTreeContainer;
					filterText.setEnabled(true);
				}
				labelStack.layout();
				contentStack.layout();
				checkInput();
			});
		}

		private void createLabelStack(Composite container) {
			labelStack = new Composite(container, SWT.NONE);
			labelStack
					.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			labelStack.setLayout(new StackLayout());
			selectProductLabel = new Label(labelStack, SWT.NONE);
			selectFlowPropertyLabel = new Label(labelStack, SWT.NONE);
			selectFlowPropertyLabel.setText(M.ReferenceFlowProperty);
		}

		private GridLayout gridLayout() {
			GridLayout layout = new GridLayout(1, true);
			layout.verticalSpacing = 0;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			layout.horizontalSpacing = 0;
			return layout;
		}

		private void createProductViewer() {
			productTreeContainer = new Composite(contentStack, SWT.NONE);
			UI.gridData(productTreeContainer, true, false);
			productTreeContainer.setLayout(gridLayout());
			productTree = NavigationTree.createViewer(productTreeContainer);
			UI.gridData(productTree.getTree(), true, true).heightHint = 200;
			productTree.addFilter(new FlowTypeFilter(FlowType.ELEMENTARY_FLOW,
					FlowType.WASTE_FLOW));
			productTree.addFilter(new EmptyCategoryFilter());
			productTree.addFilter(new ModelTextFilter(filterText, productTree));
			productTree.addSelectionChangedListener(s -> checkInput());
			productTree.setInput(Navigator.findElement(ModelType.FLOW));
		}

		private void createPropertyViewer() {
			flowPropertyContainer = new Composite(contentStack, SWT.NONE);
			UI.gridData(flowPropertyContainer, true, false);
			flowPropertyContainer.setLayout(gridLayout());
			flowPropertyViewer = new FlowPropertyViewer(flowPropertyContainer);
			flowPropertyViewer.setInput(Database.get());
			flowPropertyViewer.selectFirst();
		}

		@Override
		public Process createModel() {
			ProcessCreationController controller = new ProcessCreationController(
					Database.get());
			controller.setName(getModelName());
			controller.setCreateWithProduct(createRefFlowCheck.getSelection());
			controller.setDescription(getModelDescription());
			Flow flow = getSelectedFlow();
			if (flow != null)
				controller.setFlow(Descriptors.toDescriptor(flow));
			controller.setFlowProperty(flowPropertyViewer.getSelected());
			Process result = controller.create();
			Navigator.refresh((Navigator.findElement(ModelType.FLOW)));
			return result;
		}

		private Flow getSelectedFlow() {
			INavigationElement<?> e = Viewers.getFirstSelected(productTree);
			if (e == null || !(e.getContent() instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) e.getContent();
			IDatabase db = Database.get();
			return db.createDao(Flow.class).getForId(flow.getId());
		}
	}

}