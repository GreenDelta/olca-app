package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.app.db.Database;
import org.openlca.core.database.BaseDao;
import org.openlca.core.model.Source;

public class SourceWizard extends AbstractWizard<Source> {

	@Override
	protected String getTitle() {
		return M.NewSource;
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
			setTitle(M.NewSource);
			setMessage(M.CreatesANewSource);
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
