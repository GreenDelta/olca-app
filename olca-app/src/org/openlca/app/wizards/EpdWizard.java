package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.app.M;
import org.openlca.core.model.Epd;
import org.openlca.core.model.ModelType;

public class EpdWizard extends AbstractWizard<Epd> {

	@Override
	protected String getTitle() {
		return M.NewEpd;
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.EPD;
	}

	@Override
	protected AbstractWizardPage<Epd> createPage() {
		return new Page();
	}

	private static class Page extends AbstractWizardPage<Epd> {

		public Page() {
			super("EpdPage");
			setTitle(M.NewEpd);
			setMessage("Create a new environmental product declaration");
			setPageComplete(false);
		}

		@Override
		public Epd createModel() {
			var epd = new Epd();
			epd.refId = UUID.randomUUID().toString();
			epd.name = getModelName();
			epd.description = getModelDescription();
			return epd;
		}

	}
}
