package org.openlca.core.editors.source;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.navigation.Navigator;
import org.openlca.core.editors.INewModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.model.Source;
import org.openlca.core.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceWizard extends Wizard implements INewModelWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Category category;
	private SourceWizardPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.Sources_WizardTitle);
		setDefaultPageImageDescriptor(ImageType.NEW_WIZARD.getDescriptor());
	}

	@Override
	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public void addPages() {
		page = new SourceWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		log.trace("finish create source");
		try {
			Source source = page.getSource();
			source.setCategory(category);
			Database.createDao(Source.class).insert(source);
			App.openEditor(source);
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			log.error("failed to create source", e);
			return false;
		}
	}

}
