package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;

public class DQSystemWizard extends AbstractWizard<DQSystem> {

	@Override
	protected String getTitle() {
		return "#New data quality system";
	}

	@Override
	protected AbstractWizardPage<DQSystem> createPage() {
		return new DQSystemWizardPage();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.DQ_SYSTEM;
	}

	private class DQSystemWizardPage extends AbstractWizardPage<DQSystem> {

		private Button hasUncertaintiesCheck;

		public DQSystemWizardPage() {
			super("DQSystemWizardPage");
			setTitle("#New data quality system");
			setMessage("#Creates a new data quality system");
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
			new Label(container, SWT.NONE);
			hasUncertaintiesCheck = new Button(container, SWT.CHECK);
			hasUncertaintiesCheck.setText("#System defines uncertainties");
		}

		@Override
		public DQSystem createModel() {
			DQSystem system = new DQSystem();
			system.setRefId(UUID.randomUUID().toString());
			system.setName(getModelName());
			system.setDescription(getModelDescription());
			system.hasUncertainties = hasUncertaintiesCheck.getSelection();
			return system;
		}

	}

}
