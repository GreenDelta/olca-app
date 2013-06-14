/*******************************************************************************
 * Copyright (c) 2007 - 2010 GreenDeltaTC. All rights reserved. This program and
 * the accompanying materials are made available under the terms of the Mozilla
 * Public License v1.1 which accompanies this distribution, and is available at
 * http://www.openlca.org/uploads/media/MPL-1.1.html
 * 
 * Contributors: GreenDeltaTC - initial API and implementation
 * www.greendeltatc.com tel.: +49 30 4849 6030 mail: gdtc@greendeltatc.com
 ******************************************************************************/
package org.openlca.core.editors.process;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.core.application.Messages;
import org.openlca.core.application.navigation.INavigationElement;
import org.openlca.core.application.navigation.NavigationRoot;
import org.openlca.core.application.views.navigator.ModelWizardPage;
import org.openlca.core.application.views.navigator.Navigator;
import org.openlca.core.application.views.navigator.filter.EmptyCategoryFilter;
import org.openlca.core.application.views.navigator.filter.FlowTypeFilter;
import org.openlca.core.editors.controllers.ProcessCreationController;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.Descriptors;
import org.openlca.core.resources.ImageType;
import org.openlca.ui.UI;
import org.openlca.ui.Viewers;
import org.openlca.ui.viewer.FlowPropertyViewer;
import org.openlca.ui.viewer.ModelComponentTreeViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wizard page for creating a new process
 * 
 * @author Sebastian Greve
 * 
 */
public class ProcessWizardPage extends ModelWizardPage {

	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Composite contentStack;
	private Button createRefFlowCheck;
	private Composite labelStack;
	private TreeViewer productViewer;
	private FlowPropertyViewer flowPropertyViewer;
	private Label selectFlowLabel;
	private Label selectFlowPropertyLabel;
	private Composite flowPropertyViewerContainer;
	private Composite productViewerContainer;

	protected ProcessWizardPage() {
		super("ProcessWizardPage");
		setTitle(Messages.Processes_WizardTitle);
		setMessage(Messages.Processes_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_PROCESS.getDescriptor());
		setPageComplete(false);
	}

	private void initListeners() {

		createRefFlowCheck.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean createFlow = createRefFlowCheck.getSelection();
				StackLayout labelLayout = (StackLayout) labelStack.getLayout();
				StackLayout contentLayout = (StackLayout) contentStack
						.getLayout();
				if (createFlow) {
					labelLayout.topControl = selectFlowPropertyLabel;
					contentLayout.topControl = flowPropertyViewerContainer;
				} else {
					labelLayout.topControl = selectFlowLabel;
					contentLayout.topControl = productViewerContainer;
				}
				labelStack.layout();
				contentStack.layout();
				checkInput();
			}
		});

		productViewer
				.addSelectionChangedListener(new ISelectionChangedListener() {

					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						checkInput();
					}
				});

	}

	private void setData() {
		NavigationRoot root = Navigator.getNavigationRoot();
		if (root != null)
			productViewer.setInput(root.getCategoryRoot(Flow.class,
					getDatabase()));
		flowPropertyViewer.setInput(getDatabase());
		flowPropertyViewer.selectFirst();
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		boolean createFlow = createRefFlowCheck.getSelection();
		String err = Messages.Processes_EmptyQuantitativeReferenceError;
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
		createRefFlowCheck = new Button(container, SWT.CHECK);
		createRefFlowCheck.setText(Messages.Processes_CreateProductFlow);

		labelStack = new Composite(container, SWT.NONE);
		labelStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelStack.setLayout(new StackLayout());

		contentStack = new Composite(container, SWT.NONE);
		UI.gridData(contentStack, true, true).heightHint = 200;
		contentStack.setLayout(new StackLayout());

		selectFlowLabel = new Label(labelStack, SWT.NONE);
		selectFlowLabel.setText(Messages.Common_QuantitativeReference);

		createProductViewer();

		selectFlowPropertyLabel = new Label(labelStack, SWT.NONE);
		selectFlowPropertyLabel.setText(Messages.Flows_ReferenceFlowProperty);

		// create combo viewer for selecting a reference flow property
		flowPropertyViewerContainer = new Composite(contentStack, SWT.NONE);
		UI.gridData(flowPropertyViewerContainer, true, false);
		flowPropertyViewerContainer.setLayout(gridLayout());
		flowPropertyViewer = new FlowPropertyViewer(flowPropertyViewerContainer);

		setData();
		initListeners();

		((StackLayout) labelStack.getLayout()).topControl = selectFlowLabel;
		((StackLayout) contentStack.getLayout()).topControl = productViewerContainer;
		labelStack.layout();
		contentStack.layout();
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
		log.trace("start initialise product viewer");
		NavigationRoot root = Navigator.getNavigationRoot();
		productViewerContainer = new Composite(contentStack, SWT.NONE);
		UI.gridData(productViewerContainer, true, false);
		productViewerContainer.setLayout(gridLayout());
		productViewer = new ModelComponentTreeViewer(productViewerContainer,
				false, false, root != null ? root.getCategoryRoot(Flow.class,
						getDatabase()) : root, null);
		UI.gridData(productViewer.getTree(), true, true).heightHint = 200;
		productViewer.addFilter(new FlowTypeFilter(FlowType.ElementaryFlow,
				FlowType.WasteFlow));
		productViewer.addFilter(new EmptyCategoryFilter());
		log.trace("product viewer initialised");
	}

	@Override
	protected Object[] getData() {
		ProcessCreationController controller = new ProcessCreationController(
				getDatabase());
		controller.setCategoryId(getCategoryId());
		controller.setName(getComponentName());
		controller.setCreateWithProduct(createRefFlowCheck.getSelection());
		controller.setDescription(getComponentDescription());
		Flow flow = getSelectedFlow();
		if (flow != null)
			controller.setFlow(Descriptors.toDescriptor(flow));
		controller.setFlowProperty(flowPropertyViewer.getSelected());
		return new Object[] { controller.create() };
	}

	private Flow getSelectedFlow() {
		INavigationElement e = Viewers.getFirstSelected(productViewer);
		if (e == null || !(e.getData() instanceof Flow))
			return null;
		return (Flow) e.getData();
	}
}
