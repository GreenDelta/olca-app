package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.util.UI;
import org.openlca.app.viewers.combo.FlowPropertyCombo;
import org.openlca.app.viewers.combo.FlowTypeViewer;
import org.openlca.core.model.Flow;
import org.openlca.core.model.FlowProperty;
import org.openlca.core.model.FlowPropertyFactor;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.ModelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowWizard extends AbstractWizard<Flow> {

	@Override
	protected AbstractWizardPage<Flow> createPage() {
		return new Page();
	}

	@Override
	protected String getTitle() {
		return M.NewFlow;
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.FLOW;
	}

	private static class Page extends AbstractWizardPage<Flow> {

		private FlowTypeViewer typeCombo;
		private FlowPropertyCombo propertyCombo;

		public Page() {
			super("FlowWizardPage");
			setTitle(M.NewFlow);
			setMessage(M.CreatesANewFlow);
			setPageComplete(false);
		}

		@Override
		protected void checkInput() {
			super.checkInput();
			if (getErrorMessage() == null) {
				if (propertyCombo.getSelected() == null) {
					setErrorMessage(M.NoReferenceFlowPropertySelected);
				}
			}
			setPageComplete(getErrorMessage() == null);
		}

		@Override
		protected void modelWidgets(final Composite container) {
			UI.formLabel(container, M.FlowType);
			typeCombo = new FlowTypeViewer(container);
			typeCombo.select(FlowType.ELEMENTARY_FLOW);
			UI.formLabel(container, M.ReferenceFlowProperty);
			propertyCombo = new FlowPropertyCombo(container);
			propertyCombo.setInput(Database.get());
			propertyCombo.selectFirst();
		}

		@Override
		protected void initModifyListeners() {
			super.initModifyListeners();
			propertyCombo.addSelectionChangedListener(s -> checkInput());
		}

		@Override
		public Flow createModel() {
			Flow flow = new Flow();
			flow.refId = UUID.randomUUID().toString();
			flow.name = getModelName();
			flow.description = getModelDescription();
			flow.flowType = typeCombo.getSelected();
			addFlowProperty(flow);
			return flow;
		}

		private void addFlowProperty(Flow flow) {
			try {
				long id = propertyCombo.getSelected().id;
				FlowProperty flowProp = Cache.getEntityCache().get(
						FlowProperty.class,
						id);
				flow.referenceFlowProperty = flowProp;
				FlowPropertyFactor factor = new FlowPropertyFactor();
				factor.conversionFactor = 1;
				factor.flowProperty = flowProp;
				flow.flowPropertyFactors.add(factor);
			} catch (Exception e) {
				setErrorMessage(M.FailedToLoadFlowProperty);
				Logger log = LoggerFactory.getLogger(getClass());
				log.error("Failed to load flow property", e);
			}
		}

	}

}
