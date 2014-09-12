package org.openlca.app.wizards;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelTextFilter;
import org.openlca.app.navigation.NavigationTree;
import org.openlca.app.navigation.Navigator;
import org.openlca.app.navigation.filters.EmptyCategoryFilter;
import org.openlca.app.navigation.filters.FlowTypeFilter;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Controls;
import org.openlca.app.util.UI;
import org.openlca.app.util.Viewers;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Process;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.model.descriptors.FlowDescriptor;

class ProcessWizardPage extends AbstractWizardPage<Process> {

	private Composite contentStack;
	private Button createRefFlowCheck;
	private Composite labelStack;
	private TreeViewer productViewer;
	private FlowPropertyViewer flowPropertyViewer;
	private Label selectFlowLabel;
	private Label selectFlowPropertyLabel;
	private Composite flowPropertyViewerContainer;
	private Composite productViewerContainer;
	private Text filterText;

	protected ProcessWizardPage() {
		super("ProcessWizardPage");
		setTitle(Messages.NewProcess);
		setMessage(Messages.NewProcess);
		setImageDescriptor(ImageType.NEW_WIZ_PROCESS.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		boolean createFlow = createRefFlowCheck.getSelection();
		String err = Messages.NoQuantitativeReferenceSelected;
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
	protected void createContents(Composite container) {
		new Label(container, SWT.NONE);
		createRefFlowCheck(container);
		filterText = UI.formText(container, Messages.Filter);
		createLabelStack(container);
		contentStack = new Composite(container, SWT.NONE);
		UI.gridData(contentStack, true, true).heightHint = 200;
		contentStack.setLayout(new StackLayout());
		createProductViewer();
		createPropertyViewer();
		((StackLayout) labelStack.getLayout()).topControl = selectFlowLabel;
		((StackLayout) contentStack.getLayout()).topControl = productViewerContainer;
		labelStack.layout();
		contentStack.layout();
	}

	private void createRefFlowCheck(Composite container) {
		createRefFlowCheck = new Button(container, SWT.CHECK);
		createRefFlowCheck.setText(Messages.CreateANewProductFlowForTheProcess);
		Controls.onSelect(createRefFlowCheck, (e) -> {
			boolean createFlow = createRefFlowCheck.getSelection();
			StackLayout labelLayout = (StackLayout) labelStack.getLayout();
			StackLayout contentLayout = (StackLayout) contentStack
					.getLayout();
			if (createFlow) {
				labelLayout.topControl = selectFlowPropertyLabel;
				contentLayout.topControl = flowPropertyViewerContainer;
				filterText.setEnabled(false);
			} else {
				labelLayout.topControl = selectFlowLabel;
				contentLayout.topControl = productViewerContainer;
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
		selectFlowLabel = new Label(labelStack, SWT.NONE);
		selectFlowLabel.setText(Messages.QuantitativeReference);
		selectFlowPropertyLabel = new Label(labelStack, SWT.NONE);
		selectFlowPropertyLabel.setText(Messages.ReferenceFlowProperty);
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
		productViewerContainer = new Composite(contentStack, SWT.NONE);
		UI.gridData(productViewerContainer, true, false);
		productViewerContainer.setLayout(gridLayout());
		productViewer = NavigationTree.createViewer(productViewerContainer);
		UI.gridData(productViewer.getTree(), true, true).heightHint = 200;
		productViewer.addFilter(new FlowTypeFilter(FlowType.ELEMENTARY_FLOW,
				FlowType.WASTE_FLOW));
		productViewer.addFilter(new EmptyCategoryFilter());
		productViewer.addFilter(new ModelTextFilter(filterText, productViewer));
		productViewer.addSelectionChangedListener((s) -> checkInput());
		productViewer.setInput(Navigator.findElement(ModelType.FLOW));
	}

	private void createPropertyViewer() {
		flowPropertyViewerContainer = new Composite(contentStack, SWT.NONE);
		UI.gridData(flowPropertyViewerContainer, true, false);
		flowPropertyViewerContainer.setLayout(gridLayout());
		flowPropertyViewer = new FlowPropertyViewer(flowPropertyViewerContainer);
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
		INavigationElement<?> e = Viewers.getFirstSelected(productViewer);
		if (e == null || !(e.getContent() instanceof FlowDescriptor))
			return null;
		FlowDescriptor flow = (FlowDescriptor) e.getContent();
		IDatabase db = Database.get();
		return db.createDao(Flow.class).getForId(flow.getId());
	}
}
