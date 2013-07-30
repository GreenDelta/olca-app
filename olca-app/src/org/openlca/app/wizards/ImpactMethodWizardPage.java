package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.resources.ImageType;
import org.openlca.core.application.Messages;
import org.openlca.core.model.ImpactMethod;

class ImpactMethodWizardPage extends AbstractWizardPage<ImpactMethod> {

	public ImpactMethodWizardPage() {
		super("LCIAMethodWizardPage");
		setTitle(Messages.Methods_WizardTitle);
		setMessage(Messages.Methods_WizardMessage);
		setImageDescriptor(ImageType.NEW_WIZ_METHOD.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
	}

	@Override
	public ImpactMethod createModel() {
		ImpactMethod method = new ImpactMethod();
		method.setRefId(UUID.randomUUID().toString());
		method.setName(getModelName());
		method.setDescription(getDescription());
		return method;
	}

}
