package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.model.Source;

public class SourceWizardPage extends AbstractWizardPage<Source> {

	public SourceWizardPage() {
		super("SourceWizardPage");
		setTitle(Messages.NewSource);
		setMessage(Messages.CreatesANewSource);
		setImageDescriptor(ImageType.NEW_WIZ_SOURCE.getDescriptor());
		setPageComplete(false);
	}

	@Override
	protected void createContents(final Composite container) {
	}

	@Override
	public Source createModel() {
		Source source = new Source();
		source.setRefId(UUID.randomUUID().toString());
		source.setName(getModelName());
		source.setDescription(getModelDescription());
		return source;
	}
}
