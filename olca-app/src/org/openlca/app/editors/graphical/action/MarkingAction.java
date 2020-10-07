package org.openlca.app.editors.graphical.action;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.Action;
import org.openlca.app.M;
import org.openlca.app.editors.graphical.GraphEditor;
import org.openlca.app.editors.graphical.command.CommandUtil;
import org.openlca.app.editors.graphical.command.MarkingCommand;
import org.openlca.app.editors.graphical.model.ProcessNode;

public class MarkingAction extends Action implements UpdateAction {

	public static final String MARK_ID = "MarkingAction.MARK";
	public static final String UNMARK_ID = "MarkingAction.UNMARK";
	
	private static final int MARK = 1;
	private static final int UNMARK = 2;
	
	private final int type;
	private final GraphEditor editor;

	private List<ProcessNode> processNodes = new ArrayList<>();

	public static MarkingAction forMarking(GraphEditor editor) {
		return new MarkingAction(editor, MARK);
	}

	public static MarkingAction forUnmarking(GraphEditor editor) {
		return new MarkingAction(editor, UNMARK);
	}

	MarkingAction(GraphEditor editor, int type) {
		this.editor = editor;
		this.type = type;
		if (type == MARK) {
			setId(MARK_ID);
			setText(M.Mark);
		} else if (type == UNMARK) {
			setId(UNMARK_ID);
			setText(M.Unmark);
		}
	}

	@Override
	public void run() {
		Command cmd = null;
		for (ProcessNode node : processNodes) {
			boolean mark = type == MARK;
			if (node.isMarked() == mark)
				continue;
			cmd = CommandUtil.chain(
					new MarkingCommand(node),
					cmd);
		}
		if (cmd == null)
			return;
		editor.getCommandStack().execute(cmd);
		editor.selectionChanged(
				editor.getSite().getPart(),
				editor.getSelection());
	}

	@Override
	public void update() {
		if (editor == null) {
			setEnabled(false);
			return;
		}
		boolean mark = type == MARK;
		processNodes = GraphActions
				.allSelectedOf(editor, ProcessNode.class)
				.stream()
				.filter(node -> node.isMarked() != mark)
				.collect(Collectors.toList());
		setEnabled(!processNodes.isEmpty());
	}
}
