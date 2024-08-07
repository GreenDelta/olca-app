package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.core.model.ImpactCategory;
import org.openlca.core.model.ModelType;

public class ImpactCategoryWizard extends AbstractWizard<ImpactCategory> {

	@Override
	protected String getTitle() {
		return M.NewEnvironmentalIndicator;
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.IMPACT_CATEGORY;
	}

	@Override
	protected AbstractWizardPage<ImpactCategory> createPage() {
		return new Page();
	}

	private static class Page extends AbstractWizardPage<ImpactCategory> {

		Text refUnit;

		Page() {
			super("ImpactCategoryPage");
			setTitle(M.NewEnvironmentalIndicator);
			setPageComplete(false);
		}

		@Override
		protected void modelWidgets(Composite container) {
			refUnit = UI.labeledText(container, M.ReferenceUnit);
		}

		@Override
		public ImpactCategory createModel() {
			ImpactCategory ic = new ImpactCategory();
			ic.refId = UUID.randomUUID().toString();
			ic.name = getModelName();
			ic.description = getModelDescription();
			ic.referenceUnit = refUnit.getText();
			return ic;
		}
	}
}
