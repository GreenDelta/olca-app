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

	private String[] path() {
		if (!(getParent() instanceof DatabaseDirElement p))
			return new String[]{getContent()};
		var parentDir = p.path();
		var path = Arrays.copyOf(parentDir, parentDir.length + 1);
		path[parentDir.length] = getContent();
		return path;
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {

		return null;
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
