package org.openlca.core.editors.unitgroup;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.openlca.core.application.App;
import org.openlca.core.application.Messages;
import org.openlca.core.application.db.Database;
import org.openlca.core.application.navigation.Navigator;
import org.openlca.core.editors.INewModelWizard;
import org.openlca.core.model.Category;
import org.openlca.core.model.UnitGroup;
import org.openlca.core.resources.ImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitGroupWizard extends Wizard implements INewModelWizard {

	private Logger log = LoggerFactory.getLogger(getClass());
	private Category category;
	private UnitGroupWizardPage page;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(Messages.Units_WizardTitle);
		setDefaultPageImageDescriptor(ImageType.NEW_WIZARD.getDescriptor());
	}

	@Override
	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public void addPages() {
		page = new UnitGroupWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		log.trace("finish create unit group");
		try {
			UnitGroup unitGroup = page.getUnitGroup();
			unitGroup.setCategory(category);
			Database.createDao(UnitGroup.class).insert(unitGroup);
			App.openEditor(unitGroup);
			Navigator.refresh();
			return true;
		} catch (Exception e) {
			log.error("failed to create unit group", e);
			return false;
		}
	}

}
