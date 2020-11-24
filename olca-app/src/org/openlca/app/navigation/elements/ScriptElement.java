package org.openlca.app.navigation.elements;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.util.Dirs;

/**
 * A script element describes a folder with scripts or a single script file
 * in the navigation tree.
 */
public class ScriptElement extends NavigationElement<File> {

	public ScriptElement(INavigationElement<?> parent, File file) {
		super(parent, file);
	}

	@Override
	protected List<INavigationElement<?>> queryChilds() {
		var root = getContent();
		if (!root.isDirectory() || Dirs.isEmpty(root))
			return Collections.emptyList();
		var files = root.listFiles();
		if (files == null)
			return Collections.emptyList();
		return Arrays.stream(files)
				.filter(f -> !f.isDirectory() || !Dirs.isEmpty(f))
				.map(f -> new ScriptElement(this, f))
				.collect(Collectors.toList());
	}
}
