package org.openlca.app.collaboration.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.openlca.app.db.Database;
import org.openlca.app.db.Repository;
import org.openlca.app.navigation.elements.CategoryElement;
import org.openlca.app.navigation.elements.DatabaseElement;
import org.openlca.app.navigation.elements.GroupElement;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.navigation.elements.ModelElement;
import org.openlca.app.navigation.elements.ModelTypeElement;
import org.openlca.git.Config;
import org.openlca.git.model.Commit;
import org.openlca.git.model.Diff;
import org.openlca.git.util.DiffEntries;
import org.openlca.util.Categories;
import org.openlca.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceDiffs {

	private final static Logger log = LoggerFactory.getLogger(WorkspaceDiffs.class);

	public static List<Diff> get(Commit commit) {
		return get(commit, null);
	}

	public static List<Diff> get(Commit commit, List<INavigationElement<?>> filterElements) {
		try {
			var config = Config.newJsonConfig(Database.get(), Repository.get().workspaceIds, Repository.get().git,
					null);
			var pathFilters = toPathFilters(filterElements);
			return DiffEntries.workspace(config, commit, pathFilters).stream()
					.map(e -> new Diff(e, commit != null ? commit.id : null, null))
					.toList();
		} catch (IOException e) {
			log.error("Error getting workspace diffs", e);
			return new ArrayList<>();
		}
	}

	private static List<String> toPathFilters(List<INavigationElement<?>> filterElements) {
		if (filterElements == null || filterElements.isEmpty())
			return new ArrayList<>();
		var all = filterElements.stream()
				.filter(e -> !(e instanceof GroupElement))
				.collect(Collectors.toList());
		filterElements.stream()
				.filter(e -> e instanceof GroupElement)
				.forEach(e -> all.addAll(e.getChildren()));
		return onlyRetainTopLevel(filterElements.stream()
				.map(WorkspaceDiffs::getPath)
				.filter(Strings::notEmpty)
				.distinct()
				.toList());
	}

	private static String getPath(INavigationElement<?> element) {
		if (element instanceof DatabaseElement)
			return null;
		if (element instanceof ModelTypeElement e)
			return e.getContent().name();
		if (element instanceof CategoryElement e)
			return e.getContent().modelType.name() + Strings.join(Categories.path(e.getContent().category), '/');
		if (element instanceof ModelElement e)
			return getPath(e.getParent()) + "/" + e.getContent().refId;
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
