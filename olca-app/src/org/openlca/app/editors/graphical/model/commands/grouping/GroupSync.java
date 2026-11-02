package org.openlca.app.editors.graphical.model.commands.grouping;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;
import org.openlca.core.model.ProductSystem;

class GroupSync {

	private final ProductSystem owner;
	private final List<AnalysisGroup> groups;
	final List<AnalysisGroup> newGroups;

	private GroupSync(GroupDialog dialog) {
		this.owner = dialog.editor.getProductSystem();
		this.groups = dialog.groups;
		this.newGroups = new ArrayList<>();
	}

	static GroupSync sync(GroupDialog dialog, ProductSystem system) {
		var sync = new GroupSync(dialog);
		sync.sync(system);
		return sync;
	}

	static GroupSync syncTemplate(GroupDialog dialog, GroupTemplate template) {
		var sync = new GroupSync(dialog);
		for (var e : template.entries) {
			sync.sync(groupOf(e.name(), e.color()));
		}
		return sync;
	}

	private static AnalysisGroup groupOf(String name, String color) {
		var g = new AnalysisGroup();
		g.name = name;
		g.color = color;
		return g;
	}

	private void sync(ProductSystem system) {
		if (system == null || system.analysisGroups.isEmpty())
			return;
		for (var g : system.analysisGroups) {
			sync(g);
		}
	}

	private void sync(AnalysisGroup next) {
		if (next == null || Strings.isBlank(next.name))
			return;
		for (var g : groups) {
			if (Objects.equals(g.name, next.name)) {
				copyAttributes(next, g);
				return;
			}
		}
		var g = new AnalysisGroup();
		g.name = next.name;
		copyAttributes(next, g);
		groups.add(g);
		newGroups.add(g);
	}

	private void copyAttributes(AnalysisGroup source, AnalysisGroup target) {
		if (Strings.isBlank(target.color)) {
			target.color = source.color;
		}
		for (var p : source.processes) {
			if (target.processes.contains(p)
				|| !owner.processes.contains(p))
				continue;
			target.processes.add(p);
		}
	}
}
