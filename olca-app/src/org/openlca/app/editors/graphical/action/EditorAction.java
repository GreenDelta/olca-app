package org.openlca.app.editors.graphical.action;

import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.openlca.app.editors.graphical.ProductSystemGraphEditor;

public abstract class EditorAction extends Action implements UpdateAction {

	private ProductSystemGraphEditor editor;

	@Override
	public void update() {
		setEnabled(accept(editor.getSelection()));
	}

	void setEditor(ProductSystemGraphEditor editor) {
		this.editor = editor;
	}

	protected ProductSystemGraphEditor getEditor() {
		return editor;
	}

	protected abstract boolean accept(ISelection selection);

}
