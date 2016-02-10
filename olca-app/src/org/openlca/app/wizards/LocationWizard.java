package org.openlca.app.wizards;

import java.util.UUID;

import org.eclipse.swt.widgets.Composite;
import org.openlca.app.M;
import org.openlca.core.model.Location;
import org.openlca.core.model.ModelType;

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

		public LocationWizardPage() {
			super("LocationWizardPage");
			setTitle(M.NewLocation);
			setMessage(M.CreatesANewLocation);
			setPageComplete(false);
		}

		@Override
		protected void createContents(Composite container) {
		}

		@Override
		public Location createModel() {
			Location location = new Location();
			location.setRefId(UUID.randomUUID().toString());
			location.setName(getModelName());
			location.setDescription(getModelDescription());
			return location;
		}

	}

}