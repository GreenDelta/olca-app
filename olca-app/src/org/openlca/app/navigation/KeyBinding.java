package org.openlca.app.navigation;


import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.openlca.app.navigation.actions.DeleteMappingAction;
import org.openlca.app.navigation.actions.DeleteModelAction;
import org.openlca.app.navigation.actions.INavigationAction;
import org.openlca.app.navigation.actions.db.DbDeleteAction;
import org.openlca.app.navigation.actions.libraries.DeleteLibraryAction;
import org.openlca.app.navigation.actions.scripts.DeleteScriptAction;
import org.openlca.app.navigation.clipboard.NaviClipboard;
import org.openlca.app.navigation.elements.INavigationElement;
import org.openlca.app.util.ErrorReporter;
import org.openlca.app.viewers.Selections;

class KeyBinding extends KeyAdapter {

	private final TreeViewer tree;

	KeyBinding(TreeViewer tree) {
		this.tree = tree;
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.character == SWT.DEL && e.stateMask == 0) {
			handleDelete();
			return;
		}

		// copy, cut & paste
		if ((e.stateMask & SWT.CTRL) != 0) {
			switch (e.keyCode) {
				case 'c' -> handleCopy();
				case 'x' -> handleCut();
				case 'v' -> handlePaste();
			}
		}
	}

	private void handleCopy() {
		with(selection -> {
			if (NaviClipboard.canCopy(selection)) {
				NaviClipboard.get().copy(selection);
			}
		});
	}

	private void handleCut() {
		with(selection -> {
			if (NaviClipboard.canCut(selection)) {
				NaviClipboard.get().cut(selection);
			}
		});
	}

	private void handlePaste() {
		var clipboard = NaviClipboard.get();
		if (clipboard.isEmpty())
			return;
		with(selection -> {
			if (selection.size() != 1)
				return;
			var target = selection.getFirst();
			if (!clipboard.canPasteTo(target))
				return;
			clipboard.pasteTo(target);
		});
	}

	private void handleDelete() {
		var candidates = Stream.of(
				new DbDeleteAction(),
				new DeleteLibraryAction(),
				new DeleteModelAction(),
				new DeleteMappingAction(),
				new DeleteScriptAction());
		with(selection -> candidates
				.filter(a -> a.accept(selection))
				.findFirst()
				.ifPresent(INavigationAction::run));
	}

	private void with(Consumer<List<INavigationElement<?>>> fn) {
		var s = tree.getSelection();
		if (s == null || s.isEmpty())
			return;
		try {
			List<INavigationElement<?>> elements = Selections.allOf(s);
			if (!elements.isEmpty()) {
				fn.accept(elements);
			}
		} catch (Exception e) {
			ErrorReporter.on("failed to handle key event in navigation", e);
		}
	}
}
