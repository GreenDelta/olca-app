package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.app.M;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.Source;

public class SourceWizard extends AbstractWizard<Source> {

	@Override
	protected String getTitle() {
		return M.NewSource;
	}

	@Override
	protected AbstractWizardPage<Source> createPage() {
		return new SourceWizardPage();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.SOURCE;
	}

	private static class SourceWizardPage extends AbstractWizardPage<Source> {

		public SourceWizardPage() {
			super("SourceWizardPage");
			setTitle(M.NewSource);
			setMessage(M.CreatesANewSource);
			setPageComplete(false);
		}

		@Override
		public Source createModel() {
			Source source = new Source();
			source.refId = UUID.randomUUID().toString();
			source.name = getModelName();
			source.description = getModelDescription();
			return source;
		}
	}

}
