package org.openlca.app.navigation.elements;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.PlatformObject;
import org.openlca.app.db.Database;
import org.openlca.app.rcp.Workspace;
import org.openlca.util.Dirs;

/**
 * The root of the navigation tree. Contains the databases, libraries, scripts,
 * etc.
 */
public class NavigationRoot extends PlatformObject implements
		INavigationElement<NavigationRoot> {

	private List<INavigationElement<?>> childs;

	@Override
	public NavigationRoot getContent() {
		return this;
	}

	@Override
	public void update() {
		childs = null;
	}

	@Override
	public INavigationElement<?> getParent() {
		return null;
	}

	@Override
	public List<INavigationElement<?>> getChildren() {
		if (childs != null)
			return childs;

		childs = new ArrayList<>();

		// add database elements and their folders
		childs.addAll(dbs());

		// libraries
		var libDir = Workspace.getLibraryDir();
		var libs = libDir.getLibraries();
		if (!libs.isEmpty()) {
			childs.add(new LibraryDirElement(this, libDir));
		}

		// add a script folder if scripts are stored
		// in the workspace
		var scriptRoot = new File(Workspace.root(), "Scripts");
		if (scriptRoot.exists() && !Dirs.isEmpty(scriptRoot)) {
			childs.add(new ScriptElement(this, scriptRoot));
		}

		return childs;
	}

	private List<NavigationElement<?>> dbs() {
		var elems = new ArrayList<NavigationElement<?>>();
		var dirs = new HashMap<String, DatabaseDirElement>();
		for (var conf : Database.getConfigurations().getAll()) {
			var path = DatabaseDirElement.split(conf.category());
			if (path.length == 0) {
				elems.add(new DatabaseElement(this, conf));
				continue;
			}
			var key = path[0].toLowerCase(Locale.US);
			dirs.computeIfAbsent(key, $ -> new DatabaseDirElement(this, path[0]));
		}
		elems.addAll(dirs.values());
		return elems;
	}
}
