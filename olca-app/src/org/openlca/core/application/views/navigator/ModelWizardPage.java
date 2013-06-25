package org.openlca.core.application.views.navigator;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.core.application.Messages;
import org.openlca.ui.UIFactory;

/**
 * Abstract model component wizard page.
 */
public abstract class ModelWizardPage extends WizardPage {

	private Text descriptionText;
	private String EMPTY_NAME_ERROR = Messages.Common_PleaseEnterName;
	private Text nameText;

	protected ModelWizardPage(String pageName) {
		super(pageName);
	}

	protected void checkInput() {
		setErrorMessage(null);
		if (nameText.getText().length() == 0) {
			setErrorMessage(EMPTY_NAME_ERROR);
		}
		setPageComplete(getErrorMessage() == null);
	}

	protected abstract void createContents(Composite container);

	protected final String getComponentDescription() {
		return descriptionText != null ? descriptionText.getText() : "";
	}

	protected final String getComponentName() {
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

}
