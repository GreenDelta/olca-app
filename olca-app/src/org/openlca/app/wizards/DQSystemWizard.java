package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.openlca.app.M;
import org.openlca.core.model.DQSystem;
import org.openlca.core.model.ModelType;

public class DQSystemWizard extends AbstractWizard<DQSystem> {

	@Override
	protected String getTitle() {
		return M.NewDataQualitySystem;
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
			setTitle(M.NewDataQualitySystem);
			setMessage(M.CreatesANewDataQualitySystem);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
			new Label(container, SWT.NONE);
			hasUncertaintiesCheck = new Button(container, SWT.CHECK);
			hasUncertaintiesCheck.setText(M.SystemDefinesUncertainties);
		}

		@Override
		public DQSystem createModel() {
			DQSystem system = new DQSystem();
			system.refId = UUID.randomUUID().toString();
			system.name = getModelName();
			system.description = getModelDescription();
			system.hasUncertainties = hasUncertaintiesCheck.getSelection();
			return system;
		}

	}

}
