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
import org.openlca.app.navigation.elements.CategoryElement;
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
import org.openlca.app.util.FileType;
import org.openlca.app.util.Labels;
import org.openlca.app.util.UI;
import org.openlca.core.library.Library;
import org.openlca.core.library.LibraryDir;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.util.Categories;

public class NavigationLabelProvider extends ColumnLabelProvider
		implements ICommonLabelProvider, IColorProvider {

	private final boolean indicateRepositoryState;

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
		if (!(obj instanceof INavigationElement))
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
			String name = Labels.name(d);
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

		// for script files and folders show the full file path
		if (obj instanceof ScriptElement) {
			var file = ((ScriptElement) obj).getContent();
			return file.getAbsolutePath();
		}

		// libraries
		if (obj instanceof LibraryDirElement) {
			var libDir = ((LibraryDirElement) obj).getContent();
			return libDir.dir.getAbsolutePath();
		}
		if (obj instanceof LibraryElement) {
			var lib = ((LibraryElement) obj).getContent();
			return lib.folder.getAbsolutePath();
		}

		return getText(obj);
	}

	@Override
	public Image getImage(Object obj) {
		if (!(obj instanceof INavigationElement))
			return null;
		var elem = (INavigationElement<?>) obj;

		if (indicateRepositoryState) {
			var img = RepositoryLabel.getWithOverlay(elem);
			if (img != null)
				return img;
		}

		var content = (elem).getContent();
		if (content instanceof IDatabaseConfiguration) {
			var config = (IDatabaseConfiguration) content;
			return Database.isActive(config)
					? Icon.DATABASE.get()
					: Icon.DATABASE_DISABLED.get();
		}

		// groups and models
		if (content instanceof Group)
			return Images.get((Group) content);
		if (content instanceof ModelType)
			return Images.getForCategory((ModelType) content);
		if (content instanceof Category)
			return Images.get((Category) content);
		if (content instanceof Descriptor)
			return Images.get((Descriptor) content);

		// libraries
		if (content instanceof LibraryDir)
			return Icon.FOLDER.get();
		if (content instanceof Library)
			return Icon.DATABASE.get();

		// files and folders
		if (content instanceof File) {
			var file = (File) content;
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
		if (!(obj instanceof INavigationElement))
			return null;
		var elem = (INavigationElement<?>) obj;
		var baseText = getBaseText(elem);
		if (baseText == null)
			return null;
		if (elem instanceof DatabaseElement) {
			var config = ((DatabaseElement) elem).getContent();
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
		if (elem instanceof GroupElement)
			return ((GroupElement) elem).getContent().label;
		var content = elem.getContent();
		if (content instanceof IDatabaseConfiguration)
			return ((IDatabaseConfiguration) content).getName();
		if (content instanceof Category)
			return ((Category) content).name;
		if (content instanceof ModelType)
			return Labels.plural((ModelType) content);
		if (content instanceof Descriptor)
			return Labels.name((Descriptor) content);
		if (content instanceof LibraryDir)
			return "Libraries";
		if (content instanceof Library) {
			var info = ((Library) content).getInfo();
			return info.name + " " + info.version;
		}
		if (elem instanceof MappingDirElement)
			return "Mapping files";

		if (content instanceof File)
			return ((File) content).getName();
		if (content instanceof String)
			return (String) content;

		return content == null ? "?" : content.toString();
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
