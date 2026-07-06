package org.openlca.app.tools.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.openlca.app.util.Controls;
import org.openlca.app.util.Labels;
import org.openlca.commons.Strings;
import org.openlca.io.olca.migration.MatchingStrategy;
import org.openlca.io.olca.migration.ProviderInfo;
import org.openlca.io.olca.migration.ProviderMatch;

class MatchFilter extends ViewerFilter {

	private final List<String> query = new ArrayList<>();
	private int strategyIdx;

	private MatchFilter(TableViewer table, Text text, Combo combo) {
		text.addModifyListener(_ -> {
			fillQuery(text.getText());
			table.refresh();
		});

		Controls.onSelect(combo, _ -> {
			strategyIdx = combo.getSelectionIndex();
			table.refresh();
		});
	}

	static void on(TableViewer table, Text text, Combo combo) {
		var filter = new MatchFilter(table, text, combo);
		table.setFilters(filter);
	}

	static String[] comboItems() {
		return new String[]{
			"All strategies",
			"Exact ID",
			"Name & location",
			"Any provider",
			"Manual selection"
		};
	}

	private void fillQuery(String text) {
		query.clear();
		if (Strings.isBlank(text))
			return;
		Arrays.stream(text.split("\\s+"))
			.map(String::trim)
			.map(String::toLowerCase)
			.filter(part -> !part.isEmpty())
			.forEach(query::add);
	}

	@Override
	public boolean select(Viewer viewer, Object parent, Object elem) {
		return elem instanceof ProviderMatch match
			&& selectByStrategy(match)
			&& selectByQuery(match);
	}

	private boolean selectByQuery(ProviderMatch match) {
		return query.isEmpty()
			|| matchesQuery(match.source())
			|| matchesQuery(match.selected());
	}

	private boolean matchesQuery(ProviderInfo info) {
		if (info == null)
			return false;
		var s = Labels.name(info.provider());
		if (Strings.isBlank(s))
			return false;
		var name = s.toLowerCase(Locale.ROOT);
		for (var q : query) {
			if (!name.contains(q))
				return false;
		}
		return true;
	}

	private boolean selectByStrategy(ProviderMatch match) {
		return switch (strategyIdx) {
			case 0 -> true;
			case 1 -> match.strategy() == MatchingStrategy.BY_ID;
			case 2 -> match.strategy() == MatchingStrategy.BY_NAME;
			case 3 -> match.strategy() == MatchingStrategy.ANY;
			case 4 -> match.strategy() == null;  // = manual selection
			default -> false;
		};
	}
}
