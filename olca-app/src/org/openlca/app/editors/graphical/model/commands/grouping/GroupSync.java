package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.ProductSystem;

/// Merges analysis groups from a source, another product system or a
/// [GroupTemplate], into the groups of a [GroupDialog]. Groups are matched by
/// name: a matching group gets the processes of the source group that are also
/// part of the edited product system, and its color if it has none. Groups
/// without a match are added and collected in `newGroups`, so that the dialog
/// can render them.
@NullMarked
class GroupSync {

	private final ProductSystem owner;
	private final List<AnalysisGroup> groups;
	private final List<AnalysisGroup> newGroups = new ArrayList<>();

	GroupSync(ProductSystem owner, List<AnalysisGroup> groups) {
		this.owner = owner;
		this.groups = groups;
	}

	List<AnalysisGroup> newGroups() {
		return Collections.unmodifiableList(newGroups);
	}

	GroupSync sync(ProductSystem system) {
		for (var g : system.analysisGroups) {
			sync(g);
		}
		return this;
	}

	GroupSync sync(GroupTemplate template) {
		for (var e : template.entries) {
			sync(e.toGroup());
		}
		return this;
	}

	private void sync(AnalysisGroup next) {
		if (Strings.isBlank(next.name))
			return;
		for (var g : groups) {
			if (sameName(g, next)) {
				copyAttributes(next, g);
				return;
			}
		}
		var g = new AnalysisGroup();
		g.name = next.name.strip();
		copyAttributes(next, g);
		groups.add(g);
		newGroups.add(g);
	}

	private boolean sameName(AnalysisGroup a, AnalysisGroup b) {
		return a.name != null
			&& b.name != null
			&& a.name.strip().equalsIgnoreCase(b.name.strip());
	}

	private void copyAttributes(AnalysisGroup source, AnalysisGroup target) {
		if (Strings.isBlank(target.color)) {
			target.color = source.color;
		}
		for (var p : source.processes) {
			if (!owner.processes.contains(p) || isGrouped(p))
				continue;
			target.processes.add(p);
		}
	}

	private boolean isGrouped(long processId) {
		for (var g : groups) {
			if (g.processes.contains(processId))
				return true;
		}
		return false;
	}
}
