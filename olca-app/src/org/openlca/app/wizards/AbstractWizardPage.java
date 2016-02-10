package org.openlca.app.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.CategorizedEntity;

public abstract class AbstractWizardPage<T extends CategorizedEntity> extends
		WizardPage {

	private final String EMPTY_NAME_ERROR = M.PleaseEnterAName;
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
		setErrorMessage(EMPTY_NAME_ERROR);
		Composite container = UIFactory.createContainer(parent);
		setControl(container);
		nameText = UIFactory.createTextWithLabel(container, M.Name,
				false);
		if (withDescription)
			descriptionText = UIFactory.createTextWithLabel(container,
					M.Description, true);
		createContents(container);
		initModifyListeners();
		AbstractWizard<T> wizard = (AbstractWizard<T>) getWizard();
		setImageDescriptor(Images.wizard(wizard.getModelType()));
	}

	protected abstract void createContents(Composite container);

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
			setErrorMessage(EMPTY_NAME_ERROR);
		}
		setPageComplete(getErrorMessage() == null);
	}

	public abstract T createModel();

}
