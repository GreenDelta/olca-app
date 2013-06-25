package org.openlca.core.editors.source;

import org.eclipse.jface.wizard.Wizard;
import org.openlca.core.application.Messages;
import org.openlca.core.editors.INewModelWizard;

public class SourceWizard extends Wizard implements INewModelWizard {

	public SourceWizard() {
		super(Messages.Sources_WizardTitle, new SourceWizardPage());
	}

}
