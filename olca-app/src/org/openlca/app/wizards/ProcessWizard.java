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
import org.openlca.core.database.FlowDao;
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

		private Button createRefFlowCheck;
		private Button wasteCheck;
		private TreeViewer flowTree;
		private FlowPropertyViewer propertyCombo;
		private Label selectProductLabel;
		private Label selectFlowPropertyLabel;
		private Composite flowPropertyContainer;
		private Composite productTreeContainer;
		private Text flowText;

		private Composite labelStack;
		private Composite contentStack;

		private FlowTypeFilter wasteFilter = new FlowTypeFilter(
				FlowType.ELEMENTARY_FLOW, FlowType.PRODUCT_FLOW);
		private FlowTypeFilter productFilter = new FlowTypeFilter(
				FlowType.ELEMENTARY_FLOW, FlowType.WASTE_FLOW);

		protected Page() {
			super("ProcessWizardPage");
			setTitle(M.NewProcess);
			setMessage(M.NewProcess);
			setWithDescription(false);
			setPageComplete(false);
		}

		@Override
		protected void checkInput() {
			super.checkInput();
			boolean createFlow = createRefFlowCheck.getSelection();
			String err = M.NoQuantitativeReferenceSelected;
			if (createFlow) {
				if (propertyCombo.getSelected() == null)
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
			createWasteCheck(comp);
			createRefFlowCheck(comp);
			flowText = UI.formText(comp, M.QuantitativeReference);
			createLabelStack(comp);
			contentStack = new Composite(comp, SWT.NONE);
			UI.gridData(contentStack, true, true).heightHint = 200;
			contentStack.setLayout(new StackLayout());
			createFlowTree();
			createPropertyViewer();
			((StackLayout) labelStack.getLayout()).topControl = selectProductLabel;
			((StackLayout) contentStack.getLayout()).topControl = productTreeContainer;
			labelStack.layout();
			contentStack.layout();
			if (refFlow != null) {
				FlowDescriptor d = Descriptors.toDescriptor(refFlow);
				INavigationElement<?> e = Navigator.findElement(d);
				ISelection s = new StructuredSelection(e);
				flowTree.setSelection(s, true);
				String name = refFlow.name != null ? refFlow.name : "";
				nameText.setText(name);
				checkInput();
			}
		}

		private void createWasteCheck(Composite comp) {
			UI.filler(comp);
			wasteCheck = new Button(comp, SWT.CHECK);
			wasteCheck.setText(M.CreateAWasteTreatmentProcess);
			Controls.onSelect(wasteCheck, e -> {
				if (wasteCheck.getSelection()) {
					flowTree.removeFilter(productFilter);
					flowTree.addFilter(wasteFilter);
					flowTree.refresh();
				} else {
					flowTree.removeFilter(wasteFilter);
					flowTree.addFilter(productFilter);
					flowTree.refresh();
				}
			});
		}

		private void createRefFlowCheck(Composite comp) {
			UI.filler(comp);
			createRefFlowCheck = new Button(comp, SWT.CHECK);
			createRefFlowCheck.setText(M.CreateANewFlowForTheProcess);
			Controls.onSelect(createRefFlowCheck, e -> {
				boolean createFlow = createRefFlowCheck.getSelection();
				StackLayout labelLayout = (StackLayout) labelStack.getLayout();
				StackLayout contentLayout = (StackLayout) contentStack
						.getLayout();
				if (createFlow) {
					labelLayout.topControl = selectFlowPropertyLabel;
					contentLayout.topControl = flowPropertyContainer;
				} else {
					labelLayout.topControl = selectProductLabel;
					contentLayout.topControl = productTreeContainer;
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

		private void createFlowTree() {
			productTreeContainer = new Composite(contentStack, SWT.NONE);
			UI.gridData(productTreeContainer, true, false);
			productTreeContainer.setLayout(gridLayout());
			flowTree = NavigationTree.createViewer(productTreeContainer);
			UI.gridData(flowTree.getTree(), true, true).heightHint = 200;
			flowTree.addFilter(productFilter);
			flowTree.addFilter(new EmptyCategoryFilter());
			flowTree.addFilter(new ModelTextFilter(flowText, flowTree));
			flowTree.addSelectionChangedListener(s -> checkInput());
			flowTree.setInput(Navigator.findElement(ModelType.FLOW));
		}

		private void createPropertyViewer() {
			flowPropertyContainer = new Composite(contentStack, SWT.NONE);
			UI.gridData(flowPropertyContainer, true, false);
			flowPropertyContainer.setLayout(gridLayout());
			propertyCombo = new FlowPropertyViewer(flowPropertyContainer);
			propertyCombo.setInput(Database.get());
			propertyCombo.selectFirst();
		}

		@Override
		public Process createModel() {
			ProcessCreator creator = new ProcessCreator(Database.get());
			creator.name = getModelName();
			creator.createWithProduct = createRefFlowCheck.getSelection();
			creator.wasteProcess = wasteCheck.getSelection();
			creator.description = getModelDescription();
			creator.flow = getSelectedFlow();
			creator.flowName = flowText.getText();
			creator.flowProperty = propertyCombo.getSelected();
			Process result = creator.create();
			Navigator.refresh((Navigator.findElement(ModelType.FLOW)));
			return result;
		}

		private Flow getSelectedFlow() {
			INavigationElement<?> e = Viewers.getFirstSelected(flowTree);
			if (e == null || !(e.getContent() instanceof FlowDescriptor))
				return null;
			FlowDescriptor flow = (FlowDescriptor) e.getContent();
			IDatabase db = Database.get();
			return new FlowDao(db).getForId(flow.id);
		}
	}

}