package org.openlca.app.wizards;

import java.util.UUID;

import org.openlca.core.model.ModelType;
import org.openlca.core.model.ResultModel;

public class ResultWizard extends AbstractWizard<ResultModel> {

	@Override
	protected String getTitle() {
		return "New result";
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.RESULT;
	}

	@Override
	protected AbstractWizardPage<ResultModel> createPage() {
		return new Page();
	}

	private static class Page extends AbstractWizardPage<ResultModel> {

		private Page() {
			super("ResultWizardPage");
			setTitle("New result");
			setPageComplete(false);
		}

		@Override
		public ResultModel createModel() {
			var result = new ResultModel();
			result.refId = UUID.randomUUID().toString();
			result.name = getModelName();
			result.description = getModelDescription();
			return result;
		}
	}
}
