package org.openlca.app.results.requirements;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.util.Labels;
import org.openlca.util.Strings;

class SearchFilter extends ViewerFilter {

	// the sets contain the visible elements
	// if they are null it means that there
	// is no filter applied and all elements
	// are visible
	private HashSet<CategoryItem> visibleCategories;
	private HashSet<ProviderItem> visibleProviders;

	public SearchFilter(
		TotalRequirementsSection section,
		TreeModel model,
		Text text) {
		text.addModifyListener(e -> {
			var query = parseQuery(text.getText());
			if (query.isEmpty()) {
				visibleCategories = null;
				visibleProviders = null;
			} else {
				visibleProviders = filterProviders(model, query);
				visibleCategories = filterCategories(model, visibleProviders);
			}
			section.tree.refresh();
			section.expandFirst();
		});
	}

	private List<String> parseQuery(String s) {
		if (Strings.nullOrEmpty(s) || s.isBlank())
			return Collections.emptyList();
		var feed = s.trim().toLowerCase();
		var buffer = new StringBuilder();
		var parts = new ArrayList<String>();
		for (char c : feed.toCharArray()) {
			if (!Character.isWhitespace(c)) {
				buffer.append(c);
				continue;
			}
			if (buffer.length() > 0) {
				parts.add(buffer.toString());
				buffer.setLength(0);
			}
		}
		if (buffer.length() > 0) {
			parts.add(buffer.toString());
		}
		return parts;
	}

	private HashSet<ProviderItem> filterProviders(
		TreeModel model, List<String> query) {

		if (model == null || model.providers == null)
			return null;

		Predicate<ProviderItem> match = item -> {
			if (item.product == null)
				return false;
			var processName = Labels.name(item.product.provider());
			if (matches(processName, query))
				return true;
			var flowName = Labels.name(item.product.flow());
			return matches(flowName, query);
		};

		return model.providers.stream()
			.filter(match)
			.collect(Collectors.toCollection(HashSet::new));
	}

	private HashSet<CategoryItem> filterCategories(
		TreeModel model, HashSet<ProviderItem> providers) {

		if (model == null || model.categories == null)
			return null;

		if (providers.isEmpty())
			return new HashSet<>();

		// index all category items; top down in the trees
		var index = new HashMap<Long, CategoryItem>();
		var queue = new ArrayDeque<>(model.categories);
		while (!queue.isEmpty()) {
			var item = queue.poll();
			index.put(item.category.id, item);
			queue.addAll(item.childs);
		}

		// select the visible categories; bottom up
		// starting from the filtered providers
		var filtered = new HashSet<CategoryItem>();
		for (var provider : providers) {
			var catID = provider.categoryID();
			if (catID == null)
				continue;
			var category = index.get(catID);
			if (category != null) {
				filtered.add(category);
			}
		}

		queue = new ArrayDeque<>(filtered);
		while (!queue.isEmpty()) {
			var visible = queue.poll();
			var parentID = visible.category.category == null
				? null
				: visible.category.category.id;
			if (parentID == null)
				continue;
			var next = index.get(parentID);
			if (next == null)
				continue;
			filtered.add(next);
			queue.add(next);
		}

		return filtered;
	}

	private boolean matches(String term, List<String> query) {
		if (Strings.nullOrEmpty(term))
			return false;
		var feed = term.toLowerCase();
		for (var part : query) {
			if (!feed.contains(part))
				return false;
		}
		return true;
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object element) {
		if (!(element instanceof Item))
			return false;
		var item = (Item) element;

		// we always show child items
		if (item.isChild())
			return true;

		if (item.isProvider()) {
			return visibleProviders == null
				|| visibleProviders.contains(item.asProvider());
		}

		if (item.isCategory()) {
			return visibleCategories == null
				|| visibleCategories.contains(item.asCategory());
		}

		return false;
	}
}
