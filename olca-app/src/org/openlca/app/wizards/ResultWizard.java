package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.Result;

public class ResultWizard extends AbstractWizard<Result> {

	@Override
	protected String getTitle() {
		return "New result";
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.RESULT;
	}

	@Override
	protected AbstractWizardPage<Result> createPage() {
		return new Page();
	}

	private static class Page extends AbstractWizardPage<Result> {

		private Page() {
			super("ResultWizardPage");
			setTitle("New result");
			setPageComplete(false);
		}

		@Override
		public Result createModel() {
			var result = new Result();
			result.refId = UUID.randomUUID().toString();
			result.name = getModelName();
			result.description = getModelDescription();
			return result;
		}
	}
}
