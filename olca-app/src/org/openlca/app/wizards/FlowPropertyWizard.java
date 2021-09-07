package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyTypeViewer;
import org.openlca.app.viewers.combo.UnitGroupViewer;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyType;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.UnitGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowPropertyWizard extends AbstractWizard<FlowProperty> {

	@Override
	protected String getTitle() {
		return M.NewFlowProperty;
	}

	@Override
	protected AbstractWizardPage<FlowProperty> createPage() {
		return new Page();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.FLOW_PROPERTY;
	}

	private static class Page extends AbstractWizardPage<FlowProperty> {

		private FlowPropertyTypeViewer typeCombo;
		private UnitGroupViewer unitGroupCombo;

		public Page() {
			super("FlowPropertyWizardPage");
			setTitle(M.NewFlowProperty);
			setMessage(M.CreatesANewFlowProperty);
			setPageComplete(false);
		}

		@Override
		protected void checkInput() {
			super.checkInput();
			if (getErrorMessage() == null) {
				if (unitGroupCombo.getSelected() == null) {
					setErrorMessage(M.NoUnitGroupSelected);
				}
			}
			setPageComplete(getErrorMessage() == null);
		}

		@Override
		protected void modelWidgets(final Composite container) {
			UI.formLabel(container, M.FlowPropertyType);
			typeCombo = new FlowPropertyTypeViewer(container);
			typeCombo.select(FlowPropertyType.PHYSICAL);
			UI.formLabel(container, M.UnitGroup);
			unitGroupCombo = new UnitGroupViewer(container);
			unitGroupCombo.setInput(Database.get());
		}

		@Override
		protected void initModifyListeners() {
			super.initModifyListeners();
			unitGroupCombo.addSelectionChangedListener((s) -> checkInput());
		}

		@Override
		public FlowProperty createModel() {
			FlowProperty flowProperty = new FlowProperty();
			flowProperty.refId = UUID.randomUUID().toString();
			flowProperty.name = getModelName();
			flowProperty.description = getModelDescription();
			try {
				flowProperty.unitGroup = Cache.getEntityCache()
					.get(UnitGroup.class, unitGroupCombo.getSelected().id);
			} catch (Exception e) {
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("failed to load unit group", e);
			}
			flowProperty.flowPropertyType = typeCombo.getSelected();
			return flowProperty;
		}

	}

}
