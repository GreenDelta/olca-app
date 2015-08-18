package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.Messages;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.ImageType;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Source;

public class SourceWizard extends AbstractWizard<Source> {

	@Override
	protected String getTitle() {
		return Messages.NewSource;
	}

	@Override
	protected BaseDao<Source> createDao() {
		return Database.createDao(Source.class);
	}

	@Override
	protected AbstractWizardPage<Source> createPage() {
		return new SourceWizardPage();
	}

	private class SourceWizardPage extends AbstractWizardPage<Source> {

		public SourceWizardPage() {
			super("SourceWizardPage");
			setTitle(Messages.NewSource);
			setMessage(Messages.CreatesANewSource);
			setImageDescriptor(ImageType.NEW_WIZ_SOURCE.getDescriptor());
			setPageComplete(false);
		}

		protected void createContents(Composite container) {
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

}
