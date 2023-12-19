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
import org.openlca.app.collaboration.navigation.RepositoryLabel;
import org.openlca.app.db.Cache;
import org.openlca.app.db.Database;
import org.openlca.app.db.Libraries;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseDirElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.Group;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.LibraryDirElement;
import org.openlca.app.navigation.elements.LibraryElement;
import org.openlca.app.navigation.elements.MappingDirElement;
import org.openlca.app.navigation.elements.MappingFileElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ScriptElement;
import org.openlca.app.rcp.Workspace;
import org.openlca.app.rcp.images.Icon;
import org.openlca.app.rcp.images.Images;
import org.openlca.app.util.Colors;
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.database.config.DatabaseConfig;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Categories;

public class NavigationLabelProvider extends ColumnLabelProvider
	implements ICommonLabelProvider, IColorProvider {

	private final boolean indicateRepositoryState;

	/**
	 * The default constructor is required by the common-navigator framework.
	 */
	public NavigationLabelProvider() {
		this.indicateRepositoryState = true;
	}

	private NavigationLabelProvider(boolean indicateRepositoryState) {
		this.indicateRepositoryState = indicateRepositoryState;
	}

	public static NavigationLabelProvider withoutRepositoryState() {
		return new NavigationLabelProvider(false);
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public String getDescription(Object obj) {
		// the description is shown in the status bar
		if (!(obj instanceof INavigationElement))
			return null;

		// for local databases show the full path to the folder
		if (obj instanceof DatabaseElement elem) {
			var config = elem.getContent();
			if (config == null)
				return null;
			if (config.isEmbedded()) {
				File db = new File(Workspace.dbDir(), config.name());
				if (db.isDirectory())
					return db.getAbsolutePath();
			}
			return config.name();
		}

		// for models show the category path + name
		if (obj instanceof ModelElement elem) {
			var descriptor = elem.getContent();
			var name = Labels.name(descriptor);
			if (descriptor.category == null)
				return name;
			var category = Cache.getEntityCache().get(
				Category.class, descriptor.category);
			var text = category != null
				? String.join(" / ", Categories.path(category)) + " / " + name
				: name;
			return descriptor.isFromLibrary()
				? descriptor.library + ": " + text
				: text;
		}

		// for categories show the full path
		if (obj instanceof CategoryElement elem) {
			var category = elem.getContent();
			return category != null
				? String.join(" / ", Categories.path(category))
				: null;
		}

		// for script files and folders show the full file path
		if (obj instanceof ScriptElement elem) {
			var file = elem.getContent();
			return file != null
				? file.getAbsolutePath()
				: null;
		}

		// libraries
		if (obj instanceof LibraryDirElement elem) {
			var libDir = elem.getContent();
			return libDir != null
				? libDir.folder().getAbsolutePath()
				: null;
		}
		if (obj instanceof LibraryElement elem) {
			var lib = elem.getContent();
			return lib != null
				? lib.folder().getAbsolutePath()
				: null;
		}

		return getText(obj);
	}

	@Override
	public Image getImage(Object obj) {
		if (!(obj instanceof INavigationElement<?> elem))
			return null;

		if (indicateRepositoryState) {
			var img = RepositoryLabel.getWithOverlay(elem);
			if (img != null)
				return img;
		}

		if (elem instanceof DatabaseDirElement)
			return Icon.FOLDER.get();

		var content = (elem).getContent();
		if (content instanceof DatabaseConfig config) {
			return Database.isActive(config)
				? Icon.DATABASE.get()
				: Icon.DATABASE_DISABLED.get();
		}

		// groups and models
		if (content instanceof Group group)
			return Images.get(group);
		if (content instanceof ModelType type)
			return Images.getForCategory(type);
		if (content instanceof Category category)
			return Images.get(category);
		if (content instanceof Descriptor descriptor) {
			return Images.get(descriptor);
		}

		// libraries
		if (content instanceof LibraryDir)
			return Icon.FOLDER.get();
		if (content instanceof Library lib) {
			var license = Libraries.getLicense(lib.folder());
			return license.map(l -> Images.licensedLibrary(l.isValid()))
					.orElse(Icon.LIBRARY.get());
		}

		// files and folders
		if (content instanceof File file) {
			return file.isDirectory()
				? Icon.FOLDER.get()
				: Images.get(FileType.of(file));
		}

		// mapping files
		if (elem instanceof MappingDirElement)
			return Icon.FOLDER.get();
		if (elem instanceof MappingFileElement) {
			var name = content instanceof String
				? (String) content
				: "?";
			return Images.get(FileType.forName(name));
		}

		return null;
	}

	@Override
	public String getText(Object obj) {
		if (!(obj instanceof INavigationElement<?> elem))
			return null;
		var baseText = getBaseText(elem);
		if (baseText == null)
			return null;
		if (elem instanceof DatabaseElement dbElem) {
			var config = dbElem.getContent();
			var repoText = RepositoryLabel.getRepositoryText(config);
			if (repoText != null)
				baseText += repoText;
		}
		if (!indicateRepositoryState)
			return baseText;
		var state = RepositoryLabel.getStateIndicator(elem);
		if (state == null)
			return baseText;
		return state + baseText;
	}

	private String getBaseText(INavigationElement<?> elem) {

		if (elem instanceof DatabaseDirElement dirElem)
			return dirElem.getContent();
		if (elem instanceof GroupElement groupElem)
			return groupElem.getContent().label;

		var content = elem.getContent();
		if (content instanceof DatabaseConfig config)
			return config.name();
		if (content instanceof Category category)
			return category.name;
		if (content instanceof ModelType type)
			return Labels.plural(type);
		if (content instanceof Descriptor d)
			return Labels.name(d);
		if (content instanceof LibraryDir)
			return "Libraries";
		if (content instanceof Library lib) {
			return lib.name();
		}
		if (elem instanceof MappingDirElement)
			return "Mapping files";

		if (content instanceof File file)
			return file.getName();
		if (content instanceof String)
			return (String) content;

		return content == null ? "?" : content.toString();
	}

	@Override
	public Font getFont(Object elem) {
		if (!(elem instanceof INavigationElement<?>))
			return null;
		if (elem instanceof DatabaseElement dbElem
			&& Database.isActive(dbElem.getContent()))
			return UI.boldFont();
		return isFromLibrary(elem)
			? UI.italicFont()
			: null;
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

	@Override
	public Color getForeground(Object obj) {
		return isFromLibrary(obj)
			? Colors.get(55, 71, 79)
			: null;
	}

	private boolean isFromLibrary(Object obj) {
		if (obj instanceof ModelElement e)
			return e.isFromLibrary();
		if (obj instanceof CategoryElement e)
			return e.hasLibraryContent();
		return false;
	}

}
