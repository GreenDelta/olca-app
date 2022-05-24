package org.openlca.app.collaboration.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.util.Categories;
import org.openlca.util.Strings;

public class PathFilters {

	public static List<String> of(List<INavigationElement<?>> filterElements) {
		if (filterElements == null || filterElements.isEmpty())
			return new ArrayList<>();
		var all = filterElements.stream()
				.filter(e -> !(e instanceof GroupElement))
				.collect(Collectors.toList());
		filterElements.stream()
				.filter(e -> e instanceof GroupElement)
				.forEach(e -> all.addAll(e.getChildren()));
		return onlyRetainTopLevel(all.stream()
				.map(PathFilters::getPath)
				.filter(Strings::notEmpty)
				.distinct()
				.toList());
	}

	private static String getPath(INavigationElement<?> element) {
		if (element instanceof DatabaseElement)
			return null;
		if (element instanceof ModelTypeElement e)
			return e.getContent().name();
		if (element instanceof CategoryElement e) {
			var path = e.getContent().modelType.name() + "/" + Strings.join(Categories.path(e.getContent()), '/');
			return path;
		}
		if (element instanceof ModelElement e)
			return getPath(e.getParent()) + "/" + e.getContent().refId + ".json";
		return null;
	}

	/**
	 * only paths remain that are not children of another path, e.g.:<br>
	 * [<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path1,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path1/subPath,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path1/subPath/subSubPath,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path2/subPath,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path2/subPath/subSubPath,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path3/subPath/subSubPath<br>
	 * ] -> [<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path1,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path2/subPath,<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;path3/subPath/subSubPath<br>
	 * ]
	 */
	private static List<String> onlyRetainTopLevel(List<String> paths) {
		var conjuncted = new ArrayList<String>();
		if (paths.contains(""))
			return conjuncted;
		paths.stream()
				.sorted((p1, p2) -> Integer.compare(count(p1, '/'), count(p2, '/')))
				.forEach(path -> {
					if (conjuncted.stream().noneMatch(candidate -> path.startsWith(candidate + "/"))) {
						conjuncted.add(path);
					}
				});
		return conjuncted;
	}

	private static int count(String str, char find) {
		var count = 0;
		for (var c : str.toCharArray()) {
			if (c == find) {
				count++;
			}
		}
		return count;
	}
}
