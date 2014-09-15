package org.openlca.app.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.Messages;
import org.openlca.app.util.UIFactory;
import org.openlca.core.model.RootEntity;

public abstract class AbstractWizardPage<T extends RootEntity> extends
		WizardPage {

	protected Text descriptionText;
	private String EMPTY_NAME_ERROR = Messages.PleaseEnterAName;
	protected Text nameText;

	protected AbstractWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public final void createControl(final Composite parent) {
		setErrorMessage(EMPTY_NAME_ERROR);
		Composite container = UIFactory.createContainer(parent);
		setControl(container);
		nameText = UIFactory.createTextWithLabel(container, Messages.Name,
				false);
		descriptionText = UIFactory.createTextWithLabel(container,
				Messages.Description, true);
		createContents(container);
		initModifyListeners();
	}

	protected abstract void createContents(Composite container);

	protected final String getModelDescription() {
		return descriptionText != null ? descriptionText.getText() : "";
	}

	protected final String getModelName() {
		return nameText != null ? nameText.getText() : "";
	}

	protected void initModifyListeners() {
		nameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				checkInput();
			}
		});
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
