package org.openlca.app.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UI;
import org.openlca.core.model.RootEntity;

abstract class AbstractWizardPage<T extends RootEntity> extends
		WizardPage {

	private Text descriptionText;
	protected Text nameText;
	private boolean withDescription = true;

	protected AbstractWizardPage(String pageName) {
		super(pageName);
	}

	public final void setWithDescription(boolean withDescription) {
		this.withDescription = withDescription;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final void createControl(final Composite parent) {
		Composite container = UI.formComposite(parent);
		setControl(container);
		nameText = UI.formText(container, M.Name);
		if (withDescription)
			descriptionText = UI.formMultiText(container, M.Description);
		modelWidgets(container);
		initModifyListeners();
		AbstractWizard<T> wizard = (AbstractWizard<T>) getWizard();
		setImageDescriptor(Images.wizard(wizard.getModelType()));
	}

	/**
	 * Create additional model specific widgets.
	 *
	 * @param container the wizard container with a 2-column grid layout
	 */
	protected void modelWidgets(Composite container) {
	}

	protected final String getModelDescription() {
		return descriptionText != null ? descriptionText.getText() : "";
	}

	protected final String getModelName() {
		return nameText != null ? nameText.getText() : "";
	}

	protected void initModifyListeners() {
		nameText.addModifyListener((e) -> checkInput());
	}

	protected void checkInput() {
		setErrorMessage(null);
		if (nameText.getText().length() == 0) {
			setErrorMessage(M.PleaseEnterAName);
		}
		setPageComplete(getErrorMessage() == null);
	}

	public abstract T createModel();

}
