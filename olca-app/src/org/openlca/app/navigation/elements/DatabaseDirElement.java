package org.openlca.app.navigation.elements;

import org.openlca.app.db.Database;
import org.openlca.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseDirElement extends NavigationElement<String> {

	DatabaseDirElement(INavigationElement<?> parent, String name) {
		super(parent, name);
	}

	public String[] path() {
		if (!(getParent() instanceof DatabaseDirElement p))
			return new String[]{getContent()};
		var parentDir = p.path();
		var path = Arrays.copyOf(parentDir, parentDir.length + 1);
		path[parentDir.length] = getContent();
		return path;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var childs = new ArrayList<INavigationElement<?>>();
		var thisPath = path();
		for (var config : Database.getConfigurations().getAll()) {
			var confPath = split(config.category());
			if (!matches(confPath, thisPath))
				continue;
			if (confPath.length == thisPath.length) {
				childs.add(new DatabaseElement(this, config));
			} else {
				childs.add(new DatabaseDirElement(this, confPath[thisPath.length]));
			}
		}
		return childs;
	}

	private boolean matches(String[] confPath, String[] thisPath) {
		if (confPath.length < thisPath.length)
			return false;
		for (int i = 0; i < thisPath.length; i++) {
			if (!confPath[i].equalsIgnoreCase(thisPath[i]))
				return false;
		}
		return true;
	}

	public static String[] split(String path) {
		if (Strings.nullOrEmpty(path))
			return new String[0];
		var parts = path.split("/");
		var seg = new ArrayList<String>(parts.length);
		for (var part : parts) {
			var p = part.strip();
			if (p.isEmpty())
				continue;
			seg.add(p);
		}
		return seg.toArray(String[]::new);
	}

}
