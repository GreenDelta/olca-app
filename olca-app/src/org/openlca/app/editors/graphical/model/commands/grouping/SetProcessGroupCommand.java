package org.openlca.app.editors.graphical.model.commands.grouping;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.window.Window;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.edit.GraphEditPart;
import org.openlca.app.editors.graphical.edit.NodeEditPart;
import org.openlca.app.editors.graphical.model.Node;
import org.openlca.commons.Strings;
import org.openlca.core.model.AnalysisGroup;

public class SetProcessGroupCommand extends Command {

	private final List<NodeEditPart> nodes;
	private final List<Long> processes;

	public SetProcessGroupCommand(NodeEditPart node) {
		this(List.of(node));
	}

	public SetProcessGroupCommand(List<NodeEditPart> nodes) {
		this.nodes = nodes;
		this.processes = nodes.stream()
			.map(n -> n.getModel() != null ? n.getModel().descriptor : null)
			.filter(Objects::nonNull)
			.map(d -> d.id)
			.toList();
	}

	@Override
	public boolean canRedo() {
		return false;
	}

	@Override
	public boolean canUndo() {
		return false;
	}

	@Override
	public boolean canExecute() {
		return !nodes.isEmpty() && nodes.size() == processes.size();
	}

	@Override
	public void execute() {
		var editor = getEditor();
		if (editor == null)
			return;

		// create a copy of the current groups
		var origin = editor.getProductSystem().analysisGroups;
		var copy = new ArrayList<AnalysisGroup>(origin.size());
		for (var g : origin) {
			var c = g.copy();
			c.id = g.id;
			copy.add(c);
		}
		copy.sort((g1, g2) -> Strings.compareIgnoreCase(g1.name, g2.name));

		// edit the copy and check if something changed
		var dialog = new GroupDialog(editor, copy, getCurrent(copy));
		if (dialog.open() != Window.OK)
			return;
		boolean isCurrent = isCurrent(dialog.selected, copy);
		if (isCurrent && !dialog.groupsChanged)
			return;

		// sync changes
		origin.clear();
		origin.addAll(copy);

		if (!isCurrent) {
			applySelection(dialog.selected, copy);
		}
		if (dialog.groupsChanged) {
			applyGroupChange(origin);
		}
		editor.setDirty();
	}

	private GraphEditor getEditor() {
		if (nodes.isEmpty())
			return null;
		return nodes.getFirst().getParent() instanceof GraphEditPart gep
			? gep.getModel().editor
			: null;
	}

	private AnalysisGroup getCurrent(List<AnalysisGroup> groups) {
		return groups.stream()
			.filter(g -> g.processes.containsAll(processes))
			.findAny()
			.orElse(null);
	}

	private boolean isCurrent(AnalysisGroup g, List<AnalysisGroup> groups) {
		if (g != null)
			return g.processes.containsAll(processes);
		// the group is null -> no process can be in another group
		for (var gi : groups) {
			for (var pid : processes) {
				if (gi.processes.contains(pid))
					return false;
			}
		}
		return true;
	}

	/// Apply a newly selected group (which could be null) for the processes.
	private void applySelection(AnalysisGroup selected, List<AnalysisGroup> gs) {
		for (var g : gs) {
			processes.forEach(g.processes::remove);
		}
		if (selected != null) {
			selected.processes.addAll(processes);
		}
		for (var node : nodes) {
			node.propertyChange(new PropertyChangeEvent(
				this, Node.GROUP_PROP, null, selected));
		}
	}

	/// Handle changes, like name or color, of groups. This has an effect not
	/// only on the currently selected processes but all processes of the
	/// respective groups, their figures may need an update.
	private void applyGroupChange(List<AnalysisGroup> groups) {
		if (nodes.isEmpty())
			return;
		var parent = nodes.getFirst().getParent();
		if (parent == null)
			return;

		var map = new HashMap<Long, AnalysisGroup>();
		for (var g : groups) {
			for (var pid : g.processes) {
				map.put(pid, g);
			}
		}

		for (var c : parent.getChildren()) {
			if (!(c instanceof NodeEditPart n))
				continue;
			var model = n.getModel();
			if (model == null || model.descriptor == null)
				continue;
			var group = map.get(model.descriptor.id);
			if (group != null) {
				var evt = new PropertyChangeEvent(this, Node.GROUP_PROP, null, group);
				n.propertyChange(evt);
			}
		}
	}
}
