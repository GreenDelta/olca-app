package org.openlca.app.cloud.navigation;

import org.eclipse.swt.graphics.Image;
import org.openlca.app.cloud.index.Diff;
import org.openlca.app.cloud.index.DiffIndexer;
import org.openlca.app.cloud.index.DiffType;
import org.openlca.app.db.Database;
import org.openlca.app.navigation.CategoryElement;
import org.openlca.app.navigation.INavigationElement;
import org.openlca.app.navigation.ModelElement;
import org.openlca.app.navigation.ModelTypeElement;
import org.openlca.app.rcp.ImageManager;
import org.openlca.app.rcp.ImageType;
import org.openlca.app.util.Images;
import org.openlca.core.database.IDatabase;

import com.greendelta.cloud.api.RepositoryConfig;

public class NavigationLabelProvider extends
		org.openlca.app.navigation.NavigationLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof RepositoryElement)
			return getImage(((RepositoryElement) element).getContent());
		if (element instanceof ModelTypeElement)
			return super.getImage(element);
		DiffIndexer indexer = new DiffIndexer(
				RepositoryNavigator.getDiffIndex());
		Diff diff = indexer.getDiff(NavigationUtil
				.toDescriptor((INavigationElement<?>) element));
		if (diff.type != DiffType.NEW)
			return super.getImage(element);
		ImageType imageType = null;
		if (element instanceof CategoryElement)
			imageType = Images.getImageType(((CategoryElement) element)
					.getContent());
		else if (element instanceof ModelElement)
			imageType = Images.getImageType(((ModelElement) element)
					.getContent().getModelType());
		return ImageManager.getImageWithOverlay(imageType,
				ImageType.OVERLAY_ADDED);
	}

	@Override
	public String getText(Object elem) {
		if (!(elem instanceof INavigationElement))
			return null;
		if (Database.get() == null)
			return null;
		INavigationElement<?> element = (INavigationElement<?>) elem;
		String baseText = getBaseText(element);
		if (element instanceof NavigationRoot)
			return baseText;
		RepositoryConfig config = RepositoryNavigator.getConfig();
		if (config == null)
			return baseText;
		DiffIndexer indexer = new DiffIndexer(
				RepositoryNavigator.getDiffIndex());
		Diff diff = indexer.getDiff(NavigationUtil.toDescriptor(element));
		if (element instanceof ModelElement)
			if (hasChanged(element, diff, indexer) && !isNew(element, diff))
				return "> " + baseText;
			else
				return baseText;
		if (hasChanged(element, diff, indexer))
			return "> " + baseText;
		return baseText;
	}

	private boolean isNew(INavigationElement<?> element, Diff diff) {
		if (element instanceof RepositoryElement)
			return false;
		if (element instanceof ModelTypeElement)
			return false;
		return diff.type == DiffType.NEW;
	}

	private boolean hasChanged(INavigationElement<?> element, Diff diff,
			DiffIndexer indexer) {
		if (element instanceof RepositoryElement
				|| element instanceof ModelTypeElement) {
			for (INavigationElement<?> child : element.getChildren()) {
				Diff childDiff = indexer.getDiff(NavigationUtil
						.toDescriptor(child));
				if (hasChanged(child, childDiff, indexer))
					return true;
			}
			return false;
		}
		return diff.hasChanged() || diff.childrenHaveChanged();
	}

	private String getBaseText(INavigationElement<?> element) {
		if (element instanceof RepositoryElement)
			return getText(((RepositoryElement) element).getContent());
		return super.getText(element);
	}

	private Image getImage(RepositoryConfig config) {
		if (config == null)
			return ImageType.DB_ICON_DIS.get();
		return ImageType.DB_ICON.get();
	}

	private String getText(RepositoryConfig config) {
		IDatabase database = Database.get();
		if (config == null)
			return database.getName() + " [Not connected]";
		return "#" + database.getName() + " [" + config.getServerUrl() + " "
				+ config.getRepositoryId() + "]";
	}
}
