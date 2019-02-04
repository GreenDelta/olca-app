package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.M;
import org.openlca.app.util.UI;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;
import org.openlca.util.KeyGen;
import org.openlca.util.Strings;

public class LocationWizard extends AbstractWizard<Location> {

	@Override
	protected String getTitle() {
		return M.NewLocation;
	}

	@Override
	protected AbstractWizardPage<Location> createPage() {
		return new LocationWizardPage();
	}

	@Override
	protected ModelType getModelType() {
		return ModelType.LOCATION;
	}

	private class LocationWizardPage extends AbstractWizardPage<Location> {

		private Text codeText;
		private Text descriptionText;

		public LocationWizardPage() {
			super("LocationWizardPage");
			setTitle(M.NewLocation);
			setMessage(M.CreatesANewLocation);
			setWithDescription(false);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite comp) {
			codeText = UI.formText(comp, M.Code);
			codeText.addModifyListener(e -> checkInput());
			descriptionText = UI.formMultiText(comp, M.Description);
		}

		@Override
		public Location createModel() {
			Location location = new Location();
			String code = codeText.getText();
			if (Strings.nullOrEmpty(code)) {
				location.refId = UUID.randomUUID().toString();
			} else {
				location.code = code;
				location.refId = KeyGen.get(code);
			}
			location.name = getModelName();
			location.description = descriptionText.getText();
			return location;
		}

		@Override
		protected void checkInput() {
			super.checkInput();
			if (getErrorMessage() != null)
				return;
			if (Strings.nullOrEmpty(codeText.getText())) {
				setErrorMessage(M.ALocationCodeIsRequired);
				setPageComplete(false);
				return;
			}
		}
	}
}