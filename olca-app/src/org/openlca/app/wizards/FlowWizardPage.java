package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyViewer;
import org.openlca.app.viewers.combo.FlowTypeViewer;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowWizardPage extends AbstractWizardPage<Flow> {

	private FlowTypeViewer flowTypeViewer;
	private FlowPropertyViewer referenceFlowPropertyViewer;

	public FlowWizardPage() {
		super("FlowWizardPage");
		setTitle(Messages.NewFlow);
		setMessage(Messages.CreatesANewFlow);
		setImageDescriptor(ImageType.NEW_WIZ_FLOW.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null) {
			if (referenceFlowPropertyViewer.getSelected() == null) {
				setErrorMessage(Messages.NoReferenceFlowPropertySelected);
			}
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		UI.formLabel(container, Messages.FlowType);
		flowTypeViewer = new FlowTypeViewer(container);
		flowTypeViewer.select(FlowType.ELEMENTARY_FLOW);

		UI.formLabel(container, Messages.ReferenceFlowProperty);
		referenceFlowPropertyViewer = new FlowPropertyViewer(container);
		referenceFlowPropertyViewer.setInput(Database.get());
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		referenceFlowPropertyViewer
				.addSelectionChangedListener((s) -> checkInput());
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
			FlowProperty flowProp = Cache.getEntityCache().get(
					FlowProperty.class,
					id);
			flow.setReferenceFlowProperty(flowProp);
			FlowPropertyFactor factor = new FlowPropertyFactor();
			factor.setConversionFactor(1);
			factor.setFlowProperty(flowProp);
			flow.getFlowPropertyFactors().add(factor);
		} catch (Exception e) {
			setErrorMessage(Messages.FailedToLoadFlowProperty);
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("Failed to load flow property", e);
		}
	}

}
