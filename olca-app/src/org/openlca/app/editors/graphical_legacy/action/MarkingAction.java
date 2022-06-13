package org.openlca.app.editors.graphical_legacy.action;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.graphical_legacy.GraphEditor;
import org.openlca.app.editors.graphical_legacy.model.ProcessNode;

public class MarkingAction extends Action implements GraphAction {

	private static final int MARK = 1;
	private static final int UNMARK = 2;

	private final int type;
	private List<ProcessNode> processNodes;

	public static MarkingAction forMarking() {
		return new MarkingAction(MARK);
	}

	public static MarkingAction forUnmarking() {
		return new MarkingAction(UNMARK);
	}

	MarkingAction(int type) {
		this.type = type;
		if (type == MARK) {
			setId("MarkingAction.MARK");
			setText(M.Mark);
		} else if (type == UNMARK) {
			setId("MarkingAction.UNMARK");
			setText(M.Unmark);
		}
	}

	@Override
	public boolean accepts(GraphEditor editor) {
		boolean mark = type == MARK;
		processNodes = GraphActions
				.allSelectedOf(editor, ProcessNode.class)
				.stream()
				.filter(node -> node.isMarked() != mark)
				.collect(Collectors.toList());
		return !processNodes.isEmpty();
	}

	@Override
	public void run() {
		if (processNodes == null)
			return;
		var editor = processNodes.get(0).editor;
		editor.getGraphicalViewer().deselectAll();
		for (var node : processNodes) {
			if (type == MARK) {
				node.mark();
			} else {
				node.unmark();
			}
			node.refresh();
		}
		editor.setDirty();
	}
}
