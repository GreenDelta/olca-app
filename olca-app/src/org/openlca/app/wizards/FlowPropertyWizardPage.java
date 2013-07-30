package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.UI;
import org.openlca.app.db.Database;
import org.openlca.app.resources.ImageType;
import org.openlca.app.viewer.FlowPropertyTypeViewer;
import org.openlca.app.viewer.ISelectionChangedListener;
import org.openlca.app.viewer.UnitGroupViewer;
import org.openlca.core.application.Messages;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.model.descriptors.UnitGroupDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowPropertyWizardPage extends AbstractWizardPage<FlowProperty> {

	private FlowPropertyTypeViewer flowPropertyTypeViewer;

	private UnitGroupViewer unitGroupComboViewer;

	public FlowPropertyWizardPage() {
		super("FlowPropertyWizardPage");
		setTitle(Messages.FlowProps_WizardTitle);
		setMessage(Messages.FlowProps_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_PROPERTY.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null) {
			if (unitGroupComboViewer.getSelected() == null) {
				setErrorMessage(Messages.FlowProps_EmptyUnitGroupError);
			}
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		UI.formLabel(container, Messages.FlowProps_FlowPropertyType);
		flowPropertyTypeViewer = new FlowPropertyTypeViewer(container);
		flowPropertyTypeViewer.select(FlowPropertyType.PHYSICAL);
		UI.formLabel(container, Messages.UnitGroup);
		unitGroupComboViewer = new UnitGroupViewer(container);
		unitGroupComboViewer.setInput(Database.get());
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		unitGroupComboViewer
				.addSelectionChangedListener(new ISelectionChangedListener<UnitGroupDescriptor>() {
					@Override
					public void selectionChanged(UnitGroupDescriptor selection) {
						checkInput();
					}
				});
	}

	@Override
	public FlowProperty createModel() {
		FlowProperty flowProperty = new FlowProperty();
		flowProperty.setRefId(UUID.randomUUID().toString());
		flowProperty.setName(getModelName());
		flowProperty.setDescription(getModelDescription());
		try {
			UnitGroup unitGroup = Database.createDao(UnitGroup.class).getForId(
					unitGroupComboViewer.getSelected().getId());
			flowProperty.setUnitGroup(unitGroup);
		} catch (Exception e) {
			Logger log = LoggerFactory.getLogger(getClass());
			log.error("failed to load unit group", e);
		}
		flowProperty.setFlowPropertyType(flowPropertyTypeViewer.getSelected());
		return flowProperty;
	}

}
