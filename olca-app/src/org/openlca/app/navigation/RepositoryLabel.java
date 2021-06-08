package org.openlca.app.navigation;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.CloudUtil;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.cloud.index.DiffUtil;
import org.openlca.app.db.Database;
import org.openlca.app.db.DatabaseConfig;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.app.navigation.elements.NavigationRoot;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.rcp.images.Overlay;
import org.openlca.app.util.Colors;
import org.openlca.app.util.UI;
import org.openlca.cloud.api.RepositoryClient;

class RepositoryLabel {

	static final String CHANGED_STATE = "> ";

	static Image getWithOverlay(INavigationElement<?> e) {
		if (!Database.isConnected())
			return null;
		if (!(e instanceof ModelElement) && !(e instanceof CategoryElement))
			return null;
		var diff = DiffUtil.getDiff(CloudUtil.toDataset(e));
		if (diff == null || !diff.tracked || diff.type != DiffType.NEW)
			return null;
		if (e instanceof CategoryElement) {
			var category = ((CategoryElement) e).getContent();
			return Images.getForCategory(category.modelType, Overlay.ADDED);
		} else if (e instanceof ModelElement) {
			var model = ((ModelElement) e).getContent();
			return Images.get(model.type, Overlay.ADDED);
		}
		return null;
	}

	static String getRepositoryText(DatabaseConfig config) {
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
		if (isNew(element))
			return null;
		return CHANGED_STATE;
	}

	static Font getFont(INavigationElement<?> e) {
		if (isTracked(e))
			return null;
		return UI.italicFont();
	}

	static Color getForeground(INavigationElement<?> e) {
		if (isTracked(e))
			return null;
		return Colors.get(85, 85, 85);
	}

	private static boolean isTracked(INavigationElement<?> e) {
		if (!Database.isConnected())
			return true;
		if (e instanceof DatabaseElement)
			return true;
		if (e instanceof GroupElement)
			return true;
		if (e instanceof ModelTypeElement)
			return true;
		Diff diff = DiffUtil.getDiff(CloudUtil.toDataset(e));
		if (diff == null)
			return true;
		return diff.tracked;
	}

	private static boolean isNew(INavigationElement<?> element) {
		if (element instanceof DatabaseElement)
			return false;
		if (element instanceof GroupElement)
			return false;
		if (element instanceof ModelTypeElement)
			return false;
		Diff diff = DiffUtil.getDiff(CloudUtil.toDataset(element));
		if (diff == null)
			return true;
		return diff.type == DiffType.NEW;
	}

}
