package org.openlca.app.navigation;

import java.io.File;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.util.Categories;

public class NavigationLabelProvider extends ColumnLabelProvider
		implements ICommonLabelProvider, IColorProvider {

	private boolean indicateRepositoryState;

	public NavigationLabelProvider() {
		this(true);
	}

	public NavigationLabelProvider(boolean indicateRepositoryState) {
		this.indicateRepositoryState = indicateRepositoryState;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public String getDescription(Object obj) {
		// the description is shown in the status bar
		if (!(obj instanceof NavigationElement))
			return null;

		// for local databases show the full path to the folder
		if (obj instanceof DatabaseElement) {
			DatabaseElement elem = (DatabaseElement) obj;
			IDatabaseConfiguration config = elem.getContent();
			if (config == null)
				return null;
			if (config.isLocal()) {
				File dbDir = new File(Workspace.getDir(), "databases");
				File db = new File(dbDir, config.getName());
				if (db.isDirectory())
					return db.getAbsolutePath();
			}
			return config.getName();
		}

		// for models show the category path + name
		if (obj instanceof ModelElement) {
			ModelElement element = (ModelElement) obj;
			CategorizedDescriptor d = element.getContent();
			String name = Labels.getDisplayName(d);
			if (d.category == null)
				return name;
			Category c = Cache.getEntityCache().get(
					Category.class, d.category);
			if (c == null)
				return name;
			return String.join(" / ", Categories.path(c)) + " / " + name;
		}

		// for categories show the full path
		if (obj instanceof CategoryElement) {
			CategoryElement elem = (CategoryElement) obj;
			Category c = elem.getContent();
			if (c == null)
				return null;
			return String.join(" / ", Categories.path(c));
		}

		return getText(obj);
	}

	@Override
	public Image getImage(Object obj) {
		if (!(obj instanceof INavigationElement))
			return null;
		INavigationElement<?> elem = (INavigationElement<?>) obj;
		Image withOverlay = null;
		if (indicateRepositoryState)
			withOverlay = RepositoryLabel.getWithOverlay(elem);
		if (withOverlay != null)
			return withOverlay;
		Object content = (elem).getContent();
		if (content instanceof IDatabaseConfiguration)
			return getDatabaseImage((IDatabaseConfiguration) content);
		if (content instanceof Group)
			return Images.get((Group) content);
		if (content instanceof ModelType) {
			Category dummy = new Category();
			dummy.modelType = (ModelType) content;
			return Images.get(dummy);
		}
		if (content instanceof Category)
			return Images.get((Category) content);
		if (content instanceof BaseDescriptor)
			return Images.get((BaseDescriptor) content);
		return null;
	}

	private Image getDatabaseImage(IDatabaseConfiguration config) {
		if (Database.isActive(config))
			return Icon.DATABASE.get();
		else
			return Icon.DATABASE_DISABLED.get();
	}

	@Override
	public String getText(Object obj) {
		if (!(obj instanceof INavigationElement))
			return null;
		INavigationElement<?> elem = (INavigationElement<?>) obj;
		String baseText = getBaseText(elem);
		if (baseText == null)
			return null;
		if (elem instanceof DatabaseElement) {
			IDatabaseConfiguration config = ((DatabaseElement) elem).getContent();
			String repoText = RepositoryLabel.getRepositoryText(config);
			if (repoText != null)
				baseText += repoText;
		}
		if (!indicateRepositoryState)
			return baseText;
		String state = RepositoryLabel.getStateIndicator(elem);
		if (state == null)
			return baseText;
		return state + baseText;
	}

	private String getBaseText(INavigationElement<?> elem) {
		if (elem instanceof GroupElement)
			return ((GroupElement) elem).getContent().label;
		Object content = (elem).getContent();
		if (content instanceof IDatabaseConfiguration)
			return ((IDatabaseConfiguration) content).getName();
		if (content instanceof Category)
			return ((Category) content).name;
		if (content instanceof ModelType)
			return Labels.modelType((ModelType) content);
		if (content instanceof BaseDescriptor)
			return Labels.getDisplayName((BaseDescriptor) content);
		else
			return null;
	}

	@Override
	public Font getFont(Object elem) {
		if (!(elem instanceof INavigationElement<?>))
			return null;
		if (elem instanceof DatabaseElement) {
			DatabaseElement dbElem = (DatabaseElement) elem;
			if (Database.isActive(dbElem.getContent()))
				return UI.boldFont();
			return null;
		}
		if (!indicateRepositoryState)
			return null;
		return RepositoryLabel.getFont((INavigationElement<?>) elem);
	}

	@Override
	public Color getForeground(Object elem) {
		if (!(elem instanceof INavigationElement<?>))
			return null;
		if (!indicateRepositoryState)
			return null;
		return RepositoryLabel.getForeground((INavigationElement<?>) elem);
	}

	@Override
	public String getToolTipText(Object element) {
		return getDescription(element);
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void restoreState(IMemento aMemento) {
	}

	@Override
	public void saveState(IMemento aMemento) {
	}

}
