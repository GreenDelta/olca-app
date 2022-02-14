package org.openlca.app.wizards;

import org.openlca.core.model.Epd;
import org.openlca.core.model.ModelType;

import java.util.UUID;

public class EpdWizard extends AbstractWizard<Epd> {

	@Override
	protected String getTitle() {
		return "New EPD";
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
			setTitle("New EPD");
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
