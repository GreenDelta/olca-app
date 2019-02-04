package org.openlca.app.navigation;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;
import org.openlca.app.db.Database;
import org.openlca.app.db.IDatabaseConfiguration;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.BaseDescriptor;

public class NavigationLabelProvider extends ColumnLabelProvider
		implements ICommonLabelProvider {

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
		if (!(obj instanceof ModelElement))
			return null;
		ModelElement element = (ModelElement) obj;
		BaseDescriptor descriptor = element.getContent();
		return Labels.getDisplayInfoText(descriptor);
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
		if (!(elem instanceof DatabaseElement))
			return null;
		DatabaseElement dbElem = (DatabaseElement) elem;
		if (Database.isActive(dbElem.getContent()))
			return UI.boldFont();
		return null;
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
