package org.openlca.app.navigation;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.index.DiffUtil;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.cloud.api.RepositoryClient;
import org.openlca.core.model.Category;
import org.openlca.core.model.descriptors.CategorizedDescriptor;

class RepositoryLabel {

	static final String CHANGED_STATE = "> ";

	static Image getWithOverlay(INavigationElement<?> element) {
		if (!Database.isConnected())
			return null;
		if (element instanceof DatabaseElement)
			return null;
		if (element instanceof GroupElement)
			return null;
		if (element instanceof ModelTypeElement)
			return null;
		Diff diff = DiffUtil.getDiff(CloudUtil.toDataset(element));
		if (diff.type != DiffType.NEW)
			return null;
		if (element instanceof CategoryElement) {
			Category category = ((CategoryElement) element).getContent();
			return Images.getForCategory(category.getModelType(), Overlay.ADDED);
		} else if (element instanceof ModelElement) {
			CategorizedDescriptor model = ((ModelElement) element).getContent();
			return Images.get(model.getModelType(), Overlay.ADDED);
		}
		return null;
	}

	static String getRepositoryText(IDatabaseConfiguration config) {
		if (!Database.isActive(config))
			return null;
		RepositoryClient client = Database.getRepositoryClient();
		if (client == null)
			return null;
		return " [" + client.getConfig().getServerUrl() + " " + client.getConfig().repositoryId + "]";
	}

	static String getStateIndicator(INavigationElement<?> element) {
		if (!Database.isConnected())
			return null;
		if (element instanceof NavigationRoot)
			return null;
		RepositoryClient client = Database.getRepositoryClient();
		if (client == null)
			return null;
		boolean hasChanged = DiffUtil.hasChanged(element);
		if (!hasChanged)
			return null;
		if (element instanceof ModelElement && isNew(element))
			return null;
		return CHANGED_STATE;
	}

	private static boolean isNew(INavigationElement<?> element) {
		if (element instanceof DatabaseElement)
			return false;
		if (element instanceof GroupElement)
			return false;
		if (element instanceof ModelTypeElement)
			return false;
		Diff diff = DiffUtil.getDiff(CloudUtil.toDataset(element));
		return diff.type == DiffType.NEW;
	}

}
