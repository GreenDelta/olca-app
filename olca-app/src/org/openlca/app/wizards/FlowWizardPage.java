package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.FlowPropertyViewer;
import org.openlca.app.viewers.FlowTypeViewer;
import org.openlca.app.viewers.ISelectionChangedListener;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.descriptors.FlowPropertyDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowWizardPage extends AbstractWizardPage<Flow> {

	private FlowTypeViewer flowTypeViewer;
	private FlowPropertyViewer referenceFlowPropertyViewer;

	public FlowWizardPage() {
		super("FlowWizardPage");
		setTitle(Messages.Flows_WizardTitle);
		setMessage(Messages.Flows_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_FLOW.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null) {
			if (referenceFlowPropertyViewer.getSelected() == null) {
				setErrorMessage(Messages.Flows_EmptyReferenceFlowPropertyError);
			}
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		UI.formLabel(container, Messages.Flows_FlowType);
		flowTypeViewer = new FlowTypeViewer(container);
		flowTypeViewer.select(FlowType.ELEMENTARY_FLOW);

		UI.formLabel(container, Messages.Flows_ReferenceFlowProperty);
		referenceFlowPropertyViewer = new FlowPropertyViewer(container);
		referenceFlowPropertyViewer.setInput(Database.get());
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		referenceFlowPropertyViewer
				.addSelectionChangedListener(new ISelectionChangedListener<FlowPropertyDescriptor>() {

					@Override
					public void selectionChanged(FlowPropertyDescriptor selected) {
						checkInput();
					}

				});
	}

	@Override
	public Flow createModel() {
		Flow flow = new Flow();
		flow.setRefId(UUID.randomUUID().toString());
		flow.setName(getModelName());
		flow.setDescription(getModelDescription());
		flow.setFlowType(flowTypeViewer.getSelected());
		addFlowProperty(flow);
		return flow;
	}

	private void addFlowProperty(Flow flow) {
		try {
			long id = referenceFlowPropertyViewer.getSelected().getId();
			FlowProperty flowProp = Database.createDao(FlowProperty.class)
					.getForId(id);
			flow.setReferenceFlowProperty(flowProp);
			FlowPropertyFactor factor = new FlowPropertyFactor();
			factor.setConversionFactor(1);
			factor.setFlowProperty(flowProp);
			flow.getFlowPropertyFactors().add(factor);
		} catch (Exception e) {
			setErrorMessage("Failed to load flow property");
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to load flow property", e);
		}
	}

}
