package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyTypeViewer;
import org.openlca.app.viewers.combo.UnitGroupViewer;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlowPropertyWizardPage extends AbstractWizardPage<FlowProperty> {

	private FlowPropertyTypeViewer flowPropertyTypeViewer;

	private UnitGroupViewer unitGroupComboViewer;

	public FlowPropertyWizardPage() {
		super("FlowPropertyWizardPage");
		setTitle(Messages.NewFlowProperty);
		setMessage(Messages.CreatesANewFlowProperty);
		setImageDescriptor(ImageType.NEW_WIZ_PROPERTY.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void checkInput() {
		super.checkInput();
		if (getErrorMessage() == null) {
			if (unitGroupComboViewer.getSelected() == null) {
				setErrorMessage(Messages.NoUnitGroupSelected);
			}
		}
		setPageComplete(getErrorMessage() == null);
	}

	@Override
	protected void createContents(final Composite container) {
		UI.formLabel(container, Messages.FlowPropertyType);
		flowPropertyTypeViewer = new FlowPropertyTypeViewer(container);
		flowPropertyTypeViewer.select(FlowPropertyType.PHYSICAL);
		UI.formLabel(container, Messages.UnitGroup);
		unitGroupComboViewer = new UnitGroupViewer(container);
		unitGroupComboViewer.setInput(Database.get());
	}

	@Override
	protected void initModifyListeners() {
		super.initModifyListeners();
		unitGroupComboViewer.addSelectionChangedListener((s) -> checkInput());
	}

	@Override
	public FlowProperty createModel() {
		FlowProperty flowProperty = new FlowProperty();
		flowProperty.setRefId(UUID.randomUUID().toString());
		flowProperty.setName(getModelName());
		flowProperty.setDescription(getModelDescription());
		try {
			UnitGroup unitGroup = Cache.getEntityCache().get(UnitGroup.class,
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
