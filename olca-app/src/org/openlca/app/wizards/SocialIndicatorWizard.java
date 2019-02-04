package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.SocialIndicator;

public class SocialIndicatorWizard extends AbstractWizard<SocialIndicator> {

	@Override
	protected String getTitle() {
		return M.NewSocialIndicator;
	}

	@Override
	protected AbstractWizardPage<SocialIndicator> createPage() {
		return new Page();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.SOCIAL_INDICATOR;
	}

	private class Page extends AbstractWizardPage<SocialIndicator> {

		public Page() {
			super("SocialIndicatorWizardPage");
			setTitle(M.NewSocialIndicator);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
		}

		@Override
		public SocialIndicator createModel() {
			SocialIndicator i = new SocialIndicator();
			i.refId = UUID.randomUUID().toString();
			i.name = getModelName();
			i.description = getModelDescription();
			return i;
		}
	}

}
